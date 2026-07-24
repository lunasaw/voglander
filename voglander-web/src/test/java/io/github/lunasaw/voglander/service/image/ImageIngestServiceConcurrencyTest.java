package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseAsyncTest;
import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.client.domain.image.ImagePromoteCommand;
import io.github.lunasaw.voglander.client.domain.image.ImageStageCommand;
import io.github.lunasaw.voglander.client.domain.image.StagedImage;
import io.github.lunasaw.voglander.client.domain.image.StoredImage;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.manager.service.ImageAssetService;
import io.github.lunasaw.voglander.manager.service.ImageAssetSourceService;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;

class ImageIngestServiceConcurrencyTest extends BaseAsyncTest {

    @Autowired
    private ImageValidationService validation;
    @Autowired
    private ImageAssetManager assetManager;
    @Autowired
    private ImageAssetService assetService;
    @Autowired
    private ImageAssetSourceService sourceService;

    private String ownerId;

    @AfterEach
    void tearDown() {
        if (ownerId == null) {
            return;
        }
        List<ImageAssetDO> assets = assetService.list(new LambdaQueryWrapper<ImageAssetDO>()
            .eq(ImageAssetDO::getOwnerType, "USER")
            .eq(ImageAssetDO::getOwnerId, ownerId));
        List<String> assetIds = new ArrayList<String>();
        for (ImageAssetDO asset : assets) {
            assetIds.add(asset.getAssetId());
        }
        if (!assetIds.isEmpty()) {
            sourceService.remove(new LambdaQueryWrapper<ImageAssetSourceDO>()
                .in(ImageAssetSourceDO::getAssetId, assetIds));
            assetService.remove(new LambdaQueryWrapper<ImageAssetDO>().in(ImageAssetDO::getAssetId, assetIds));
        }
    }

    @Test
    void sameContent_shouldKeepOneAssetSourceAndFinalObject() throws Exception {
        String suffix = suffix();
        ownerId = "image-concurrency-same-" + suffix;
        byte[] bytes = png(Color.BLUE);
        InMemoryStorage storage = new InMemoryStorage();
        ImageIngestService service = service(storage);
        ImageIngestCommand command = command(ownerId, "key-" + suffix, "same.png");

        List<Attempt> attempts = race(service, command, bytes, bytes);

        assertTrue(attempts.get(0).error == null, String.valueOf(attempts.get(0).error));
        assertTrue(attempts.get(1).error == null, String.valueOf(attempts.get(1).error));
        assertEquals(attempts.get(0).asset.getAssetId(), attempts.get(1).asset.getAssetId());
        assertDatabaseAndStorageFacts(storage);
    }

    @Test
    void differentContent_shouldKeepOneWinnerAndReturnStableConflict() throws Exception {
        String suffix = suffix();
        ownerId = "image-concurrency-conflict-" + suffix;
        InMemoryStorage storage = new InMemoryStorage();
        ImageIngestService service = service(storage);
        ImageIngestCommand command = command(ownerId, "key-" + suffix, "same.png");

        List<Attempt> attempts = race(service, command, png(Color.RED), png(Color.GREEN));

        int successes = 0;
        int conflicts = 0;
        for (Attempt attempt : attempts) {
            if (attempt.asset != null) {
                successes++;
            } else if (attempt.error instanceof ServiceException
                && ((ServiceException) attempt.error).getCode()
                    == ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED.getCode()) {
                conflicts++;
            }
        }
        assertEquals(1, successes);
        assertEquals(1, conflicts);
        assertDatabaseAndStorageFacts(storage);
    }

    private void assertDatabaseAndStorageFacts(InMemoryStorage storage) {
        List<ImageAssetDO> assets = assetService.list(new LambdaQueryWrapper<ImageAssetDO>()
            .eq(ImageAssetDO::getOwnerType, "USER")
            .eq(ImageAssetDO::getOwnerId, ownerId));
        assertEquals(1, assets.size());
        assertEquals(1L, sourceService.count(new LambdaQueryWrapper<ImageAssetSourceDO>()
            .eq(ImageAssetSourceDO::getAssetId, assets.get(0).getAssetId())));
        assertEquals(Collections.singleton(assets.get(0).getStorageKey()), storage.finalKeys());
        assertEquals(0, storage.stagedCount());
    }

    private ImageIngestService service(InMemoryStorage storage) {
        ImageProperties properties = new ImageProperties();
        properties.getStorage().setWorkerNode("concurrency-test");
        properties.getStorage().setMaxUploadBytes(1024 * 1024);
        properties.getCollection().setMaxPixels(1000);
        return new ImageIngestService(storage, validation, assetManager, properties, new ImageOrphanRecorder());
    }

    private List<Attempt> race(ImageIngestService service, ImageIngestCommand command,
        byte[] first, byte[] second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Attempt> left = executor.submit(() -> attempt(service, command, first, ready, start));
            Future<Attempt> right = executor.submit(() -> attempt(service, command, second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<Attempt> attempts = new ArrayList<Attempt>();
            attempts.add(left.get(15, TimeUnit.SECONDS));
            attempts.add(right.get(15, TimeUnit.SECONDS));
            return attempts;
        } finally {
            executor.shutdownNow();
        }
    }

    private static Attempt attempt(ImageIngestService service, ImageIngestCommand command, byte[] bytes,
        CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Attempt(null, new IllegalStateException("race start timed out"));
            }
            return new Attempt(service.ingestUpload(command, new ByteArrayInputStream(bytes)), null);
        } catch (Throwable error) {
            return new Attempt(null, error);
        }
    }

    private static ImageIngestCommand command(String owner, String key, String filename) {
        return new ImageIngestCommand("USER", owner, null, key, filename, "image/png", filename);
    }

    private static byte[] png(Color color) throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String suffix() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static final class Attempt {
        private final ImageAssetDTO asset;
        private final Throwable error;

        private Attempt(ImageAssetDTO asset, Throwable error) {
            this.asset = asset;
            this.error = error;
        }
    }

    private static final class InMemoryStorage implements ImageStorageService {
        private final Map<String, byte[]> staged = new ConcurrentHashMap<String, byte[]>();
        private final Map<String, byte[]> finals = new ConcurrentHashMap<String, byte[]>();

        @Override
        public StagedImage stage(ImageStageCommand command, InputStream content) throws IOException {
            byte[] bytes = content.readAllBytes();
            String key = ".staging/" + command.workerNode() + "/" + command.assetId();
            staged.put(key, bytes);
            return new StagedImage(key, bytes.length, sha256(bytes));
        }

        @Override
        public InputStream openStaged(String stagingKey) throws IOException {
            byte[] bytes = staged.get(stagingKey);
            if (bytes == null) {
                throw new IOException("staged object not found");
            }
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public StoredImage promote(ImagePromoteCommand command) throws IOException {
            byte[] bytes = staged.remove(command.stagingKey());
            if (bytes == null) {
                throw new IOException("staged object not found");
            }
            finals.put(command.finalKey(), bytes);
            return new StoredImage(command.finalKey(), bytes.length);
        }

        @Override
        public ImageContent open(String storageKey) throws IOException {
            byte[] bytes = finals.get(storageKey);
            if (bytes == null) {
                throw new IOException("final object not found");
            }
            return new ImageContent(new ByteArrayInputStream(bytes), bytes.length);
        }

        @Override
        public boolean delete(String storageKey) {
            return finals.remove(storageKey) != null;
        }

        @Override
        public boolean exists(String storageKey) {
            return finals.containsKey(storageKey);
        }

        @Override
        public void discardStaged(String stagingKey) {
            staged.remove(stagingKey);
        }

        private Set<String> finalKeys() {
            return Collections.unmodifiableSet(finals.keySet());
        }

        private int stagedCount() {
            return staged.size();
        }

        private static String sha256(byte[] bytes) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
                StringBuilder result = new StringBuilder(digest.length * 2);
                for (byte value : digest) {
                    result.append(String.format("%02x", value & 0xff));
                }
                return result.toString();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
