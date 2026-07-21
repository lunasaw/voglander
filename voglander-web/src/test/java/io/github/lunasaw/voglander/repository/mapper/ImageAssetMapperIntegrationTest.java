package io.github.lunasaw.voglander.repository.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;

class ImageAssetMapperIntegrationTest extends BaseTest {
    @Autowired private ImageAssetMapper assetMapper;
    @Autowired private ImageAssetSourceMapper sourceMapper;
    @Autowired private ImageCollectionConfigMapper configMapper;

    @Test
    void imageIdentityAndNullableExecutionUniqueConstraints_shouldBeIdempotent() {
        String suffix = Long.toHexString(System.nanoTime());
        ImageAssetDO first = asset("img_" + suffix + "a", "idem-" + suffix);
        assertEquals(1, assetMapper.insertIfAbsent(first));
        assertEquals(0, assetMapper.insertIfAbsent(asset(first.getAssetId(), first.getIdempotencyKey())));

        ImageAssetSourceDO nullExecutionA = source(first.getAssetId(), "source-a-" + suffix, null);
        ImageAssetSourceDO nullExecutionB = source("img_" + suffix + "b", "source-b-" + suffix, null);
        assertEquals(1, sourceMapper.insertIfAbsent(nullExecutionA));
        assertEquals(1, sourceMapper.insertIfAbsent(nullExecutionB));

        ImageAssetSourceDO duplicateExecution = source("img_" + suffix + "c", "source-c-" + suffix,
            "bexec-" + suffix);
        assertEquals(1, sourceMapper.insertIfAbsent(duplicateExecution));
        ImageAssetSourceDO replay = source("img_" + suffix + "d", "source-d-" + suffix,
            "bexec-" + suffix);
        assertEquals(0, sourceMapper.insertIfAbsent(replay));
    }

    @Test
    void assetPage_shouldApplyDeviceAndChannelTogetherWithStableOrder() {
        String suffix = Long.toHexString(System.nanoTime());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        ImageAssetDO matching = asset("img_" + suffix + "a", null);
        matching.setCapturedAt(now);
        ImageAssetDO otherChannel = asset("img_" + suffix + "b", null);
        otherChannel.setCapturedAt(now.plusSeconds(1));
        assetMapper.insert(matching);
        assetMapper.insert(otherChannel);
        sourceMapper.insert(source(matching.getAssetId(), "device-" + suffix + ":channel-1", null));
        sourceMapper.insert(source(otherChannel.getAssetId(), "device-" + suffix + ":channel-2", null));

        Page<ImageAssetDO> result = assetMapper.selectPageByCondition(new Page<>(1, 10), "USER", "owner-" + suffix,
            null, null, null, "CAMERA_CAPTURE", null, null, "device-" + suffix, "channel-1", null, null);

        assertEquals(1, result.getTotal());
        assertEquals(matching.getAssetId(), result.getRecords().get(0).getAssetId());
    }

    @Test
    void assetPage_shouldExcludeDeletedAssetsUnlessStatusIsExplicitlyRequested() {
        String suffix = Long.toHexString(System.nanoTime());
        ImageAssetDO deleted = asset("img_" + suffix + "a", null);
        deleted.setStatus("DELETED");
        assetMapper.insert(deleted);

        Page<ImageAssetDO> visible = assetMapper.selectPageByCondition(new Page<>(1, 10), "USER",
            "owner-" + suffix, null, null, null, null, null, null, null, null, null, null);
        Page<ImageAssetDO> explicit = assetMapper.selectPageByCondition(new Page<>(1, 10), "USER",
            "owner-" + suffix, null, null, "DELETED", null, null, null, null, null, null, null);

        assertEquals(0, visible.getTotal());
        assertEquals(1, explicit.getTotal());
    }

    @Test
    void collectionConfigTaskIdentity_shouldBeUniqueAndReplaySafe() {
        String suffix = Long.toHexString(System.nanoTime());
        ImageCollectionConfigDO config = new ImageCollectionConfigDO();
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(config.getCreateTime());
        config.setTaskId("btask-" + suffix);
        config.setDeviceId("device-" + suffix);
        config.setChannelId("channel-1");
        config.setRetentionPolicy("PERMANENT");
        config.setVersion(0);

        assertEquals(1, configMapper.insertIfAbsent(config));
        assertEquals(0, configMapper.insertIfAbsent(config));
        assertEquals(config.getTaskId(), configMapper.selectByTaskId(config.getTaskId()).getTaskId());
    }

    private static ImageAssetDO asset(String assetId, String idempotencyKey) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        ImageAssetDO asset = new ImageAssetDO();
        asset.setCreateTime(now);
        asset.setUpdateTime(now);
        asset.setAssetId(assetId);
        asset.setAssetName("asset-" + assetId);
        asset.setStatus("AVAILABLE");
        asset.setStorageProvider("LOCAL");
        asset.setStorageKey("images/2026/07/15/" + assetId + ".jpg");
        asset.setContentType("image/jpeg");
        asset.setImageFormat("JPEG");
        asset.setFileSize(1L);
        asset.setWidth(1);
        asset.setHeight(1);
        asset.setChecksumAlgorithm("SHA256");
        asset.setChecksum("checksum-" + assetId);
        asset.setCapturedAt(now);
        asset.setIngestedAt(now);
        asset.setOwnerType("USER");
        asset.setOwnerId("owner-" + assetId.substring(4, assetId.length() - 1));
        asset.setIdempotencyKey(idempotencyKey);
        asset.setRetentionPolicy("PERMANENT");
        asset.setVersion(0);
        return asset;
    }

    private static ImageAssetSourceDO source(String assetId, String entityId, String executionId) {
        ImageAssetSourceDO source = new ImageAssetSourceDO();
        source.setCreateTime(LocalDateTime.now().withNano(0));
        source.setAssetId(assetId);
        source.setSourceType("CAMERA_CAPTURE");
        source.setSourceSystem("TEST");
        source.setSourceEntityType("CAMERA");
        source.setSourceEntityId(entityId);
        source.setSourceExecutionId(executionId);
        return source;
    }
}
