package io.github.lunasaw.voglander.service.image;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.common.enums.image.ThumbnailProfile;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import jakarta.annotation.PreDestroy;

/** Coordinates ETag, bounded transformation, timeout and memory caching. */
@Service
public class ImageThumbnailService implements AutoCloseable {

    private final ImageAssetReadService readService;
    private final ImageThumbnailTransformer transformer;
    private final ImageProperties properties;
    private final ThumbnailMemoryCache cache;
    private final ThreadPoolExecutor executor;
    private final ImageDomainMetrics metrics;

    public ImageThumbnailService(ImageAssetReadService readService, ImageThumbnailTransformer transformer,
        ImageProperties properties) {
        this(readService, transformer, properties, null);
    }

    @Autowired
    public ImageThumbnailService(ImageAssetReadService readService, ImageThumbnailTransformer transformer,
        ImageProperties properties, ImageDomainMetrics metrics) {
        this.readService = readService;
        this.transformer = transformer;
        this.properties = properties;
        this.metrics = metrics;
        ImageProperties.Thumbnail thumbnail = properties.getThumbnail();
        thumbnail.validate();
        this.cache = new ThumbnailMemoryCache(thumbnail.getCacheMaxBytes(), thumbnail.getCacheMaxEntries(),
            thumbnail.getCacheTtlSeconds());
        this.executor = new ThreadPoolExecutor(thumbnail.getWorkerCount(), thumbnail.getWorkerCount(), 0L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(thumbnail.getQueueCapacity()),
            new ThumbnailThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }

    public ImageThumbnailResult get(String assetId, String profileValue, String ifNoneMatch) {
        long started = System.nanoTime();
        ThumbnailProfile profile = null;
        String outcome = "ERROR";
        String errorCode = null;
        long outputBytes = -1L;
        try {
            profile = ThumbnailProfile.parse(profileValue);
            ImageAssetDTO asset = readService.requireReadable(assetId);
            ImageProperties.Thumbnail thumbnail = properties.getThumbnail();
            if (!thumbnail.isEnabled()) throw unavailable();
            validateSourceBounds(asset);
            String etag = deriveEtag(asset.getChecksum(), profile, thumbnail.getAlgorithmVersion());
            if (etag.equals(ifNoneMatch)) {
                outcome = "NOT_MODIFIED";
                return ImageThumbnailResult.notModified(etag, profile);
            }
            byte[] cached = cache.get(etag);
            if (cached != null) {
                outcome = "CACHE_HIT";
                outputBytes = cached.length;
                if (metrics != null) metrics.thumbnailCacheHit(profile.getValue());
                return ImageThumbnailResult.content(cached, etag, profile);
            }

            Future<byte[]> future;
            try {
                final ThumbnailProfile transformProfile = profile;
                future = executor.submit(() -> {
                    try (ImageContent content = readService.open(asset)) {
                        return transformer.transform(content, transformProfile, thumbnail.getMaxWorkingPixels(),
                            properties.getStorage().getMaxUploadBytes(), properties.getCollection().getMaxPixels());
                    }
                });
            } catch (RejectedExecutionException exception) {
                outcome = "QUEUE_REJECTED";
                if (metrics != null) metrics.thumbnailQueueRejected(profile.getValue());
                throw unavailable();
            }
            byte[] transformed;
            try {
                transformed = future.get(thumbnail.getTimeoutMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException exception) {
                outcome = "TIMEOUT";
                future.cancel(true);
                if (metrics != null) metrics.thumbnailTimeout(profile.getValue());
                throw unavailable();
            } catch (InterruptedException exception) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw unavailable();
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof ServiceException) throw (ServiceException)cause;
                throw unavailable();
            }
            if (transformed == null || transformed.length == 0 || transformed.length > profile.getMaxBytes()) {
                throw unavailable();
            }
            cache.put(etag, transformed);
            outcome = "GENERATED";
            outputBytes = transformed.length;
            return ImageThumbnailResult.content(transformed, etag, profile);
        } catch (ServiceException exception) {
            errorCode = String.valueOf(exception.getCode());
            throw exception;
        } finally {
            if (metrics != null) {
                String metricProfile = profile == null ? null : profile.getValue();
                metrics.thumbnailRequest(metricProfile, outcome, errorCode,
                    Duration.ofNanos(System.nanoTime() - started));
                if (outputBytes >= 0) metrics.thumbnailOutput(metricProfile, outputBytes);
            }
        }
    }

    public static String deriveEtag(String checksum, ThumbnailProfile profile, String algorithmVersion) {
        if (checksum == null || profile == null || algorithmVersion == null) throw unavailableStatic();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest((checksum + "\n" + profile.getValue() + "\n" + algorithmVersion)
                .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(value.length * 2);
            for (byte item : value) hex.append(String.format("%02x", item & 0xff));
            return "\"sha256:" + hex + "\"";
        } catch (Exception exception) {
            throw unavailableStatic();
        }
    }

    private void validateSourceBounds(ImageAssetDTO asset) {
        Long fileSize = asset.getFileSize();
        if (fileSize != null && (fileSize.longValue() < 0
            || fileSize.longValue() > properties.getStorage().getMaxUploadBytes())) {
            throw unavailable();
        }
        Integer width = asset.getWidth();
        Integer height = asset.getHeight();
        if (width == null && height == null) return;
        if (width == null || height == null || width.intValue() <= 0 || height.intValue() <= 0
            || (long)width.intValue() * height.intValue() > properties.getCollection().getMaxPixels()) {
            throw unavailable();
        }
    }

    @Override
    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }

    private ServiceException unavailable() {
        return unavailableStatic();
    }

    private static ServiceException unavailableStatic() {
        return new ServiceException(ServiceExceptionEnum.IMAGE_THUMBNAIL_UNAVAILABLE);
    }

    private static final class ThumbnailThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "image-thumbnail-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
