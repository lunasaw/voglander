package io.github.lunasaw.voglander.service.image;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.common.enums.image.ThumbnailProfile;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ImageThumbnailResourceLimitTest {

    private final List<ImageThumbnailService> services = new ArrayList<>();
    private final List<ExecutorService> callers = new ArrayList<>();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void cleanUp() {
        services.forEach(ImageThumbnailService::close);
        callers.forEach(ExecutorService::shutdownNow);
    }

    @Test
    void fullQueueRejectsImmediatelyWithoutGrowingWorkersOrQueue() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ImageThumbnailTransformer transformer = blockingTransformer(entered, release, null, null);
        ImageThumbnailService service = service(transformer, properties(1, 1, 30_000));
        ExecutorService caller = callers(2);

        Future<ImageThumbnailResult> running = caller.submit(() -> service.get("img-running", "table", null));
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        Future<ImageThumbnailResult> queued = caller.submit(() -> service.get("img-queued", "table", null));
        ThreadPoolExecutor thumbnailExecutor = executor(service);
        await().atMost(Duration.ofSeconds(2)).until(() -> thumbnailExecutor.getQueue().size() == 1);

        assertThumbnailUnavailable(() -> service.get("img-rejected", "table", null));
        assertEquals(1, thumbnailExecutor.getPoolSize());
        assertEquals(1, thumbnailExecutor.getQueue().size());
        assertEquals(1.0, meterRegistry.get("image_thumbnail_queue_rejections_total")
            .tag("profile", "table").counter().count());

        release.countDown();
        running.get(2, TimeUnit.SECONDS);
        queued.get(2, TimeUnit.SECONDS);
    }

    @Test
    void transformTimeoutCancelsWorkerAndReturnsStableUnavailableCode() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        ImageThumbnailService service = service(blockingTransformer(entered, release, interrupted, null),
            properties(1, 1, 100));

        assertThumbnailUnavailable(() -> service.get("img-timeout", "table", null));

        assertTrue(entered.await(1, TimeUnit.SECONDS));
        assertTrue(interrupted.await(2, TimeUnit.SECONDS));
        await().atMost(Duration.ofSeconds(2)).until(() -> executor(service).getActiveCount() == 0);
        assertEquals(1.0, meterRegistry.get("image_thumbnail_timeouts_total")
            .tag("profile", "table").counter().count());
    }

    @Test
    void slowStorageOpenIsCoveredByTheSameTimeoutAndCancellation() throws Exception {
        ImageAssetReadService readService = readService();
        CountDownLatch opening = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        when(readService.open(any())).thenAnswer(invocation -> {
            opening.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException exception) {
                interrupted.countDown();
                throw exception;
            }
            return content();
        });
        ImageThumbnailService service = service(readService, mock(ImageThumbnailTransformer.class),
            properties(1, 1, 100));

        assertThumbnailUnavailable(() -> service.get("img-slow-storage", "table", null));

        assertTrue(opening.await(1, TimeUnit.SECONDS));
        assertTrue(interrupted.await(2, TimeUnit.SECONDS));
    }

    @Test
    void concurrentTransformsNeverExceedConfiguredWorkerCount() throws Exception {
        CountDownLatch twoWorkersEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        ImageThumbnailService service = service(
            blockingTransformer(twoWorkersEntered, release, null, new AtomicInteger[] {active, maximum}),
            properties(2, 2, 5_000));
        ExecutorService caller = callers(4);
        List<Future<ImageThumbnailResult>> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int sequence = i;
            results.add(caller.submit(() -> service.get("img-concurrent-" + sequence, "gallery", null)));
        }

        assertTrue(twoWorkersEntered.await(2, TimeUnit.SECONDS));
        assertEquals(2, maximum.get());
        assertEquals(2, executor(service).getMaximumPoolSize());
        release.countDown();
        for (Future<ImageThumbnailResult> result : results) result.get(2, TimeUnit.SECONDS);
        assertTrue(maximum.get() <= 2);
    }

    @Test
    void impossibleSourceDimensionsAreRejectedBeforeStorageOrDecode() throws Exception {
        ImageAssetReadService readService = readService();
        ImageAssetDTO oversized = asset("img-oversized");
        oversized.setWidth(100_000);
        oversized.setHeight(100_000);
        when(readService.requireReadable("img-oversized")).thenReturn(oversized);
        ImageThumbnailTransformer transformer = mock(ImageThumbnailTransformer.class);
        ImageThumbnailService service = service(readService, transformer, properties(1, 1, 1000));

        assertThumbnailUnavailable(() -> service.get("img-oversized", "table", null));

        verify(readService, never()).open(any());
        verify(transformer, never()).transform(any(), any(), anyLong(), anyLong(), anyLong());
    }

    private ImageThumbnailTransformer blockingTransformer(CountDownLatch entered, CountDownLatch release,
        CountDownLatch interrupted, AtomicInteger[] concurrency) {
        ImageThumbnailTransformer transformer = mock(ImageThumbnailTransformer.class);
        when(transformer.transform(any(), any(), anyLong(), anyLong(), anyLong())).thenAnswer(invocation -> {
            if (concurrency != null) {
                int current = concurrency[0].incrementAndGet();
                concurrency[1].accumulateAndGet(current, Math::max);
            }
            entered.countDown();
            try {
                release.await();
                return new byte[] {1, 2, 3};
            } catch (InterruptedException exception) {
                if (interrupted != null) interrupted.countDown();
                throw exception;
            } finally {
                if (concurrency != null) concurrency[0].decrementAndGet();
            }
        });
        return transformer;
    }

    private ImageThumbnailService service(ImageThumbnailTransformer transformer, ImageProperties properties) {
        return service(readService(), transformer, properties);
    }

    private ImageThumbnailService service(ImageAssetReadService readService, ImageThumbnailTransformer transformer,
        ImageProperties properties) {
        ImageThumbnailService service = new ImageThumbnailService(readService, transformer, properties,
            new ImageDomainMetrics(meterRegistry));
        services.add(service);
        return service;
    }

    private ImageAssetReadService readService() {
        ImageAssetReadService readService = mock(ImageAssetReadService.class);
        when(readService.requireReadable(anyString())).thenAnswer(invocation -> asset(invocation.getArgument(0)));
        try {
            when(readService.open(any())).thenAnswer(invocation -> content());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        return readService;
    }

    private ImageAssetDTO asset(String assetId) {
        ImageAssetDTO asset = new ImageAssetDTO();
        asset.setAssetId(assetId);
        asset.setStatus("AVAILABLE");
        asset.setChecksum("checksum-" + assetId);
        asset.setStorageKey("private/" + assetId);
        asset.setFileSize(1024L);
        asset.setWidth(1920);
        asset.setHeight(1080);
        return asset;
    }

    private ImageContent content() {
        return new ImageContent(new ByteArrayInputStream(new byte[] {1}), 1);
    }

    private ImageProperties properties(int workers, int queue, long timeoutMillis) {
        ImageProperties properties = new ImageProperties();
        properties.getThumbnail().setWorkerCount(workers);
        properties.getThumbnail().setQueueCapacity(queue);
        properties.getThumbnail().setTimeoutMillis(timeoutMillis);
        return properties;
    }

    private ExecutorService callers(int threads) {
        ExecutorService caller = Executors.newFixedThreadPool(threads);
        callers.add(caller);
        return caller;
    }

    private ThreadPoolExecutor executor(ImageThumbnailService service) {
        return (ThreadPoolExecutor)ReflectionTestUtils.getField(service, "executor");
    }

    private void assertThumbnailUnavailable(ThrowingCall call) {
        ServiceException error = assertThrows(ServiceException.class, call::run);
        assertEquals(ServiceExceptionEnum.IMAGE_THUMBNAIL_UNAVAILABLE.getCode(), error.getCode());
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws Exception;
    }
}
