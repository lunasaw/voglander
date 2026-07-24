package io.github.lunasaw.voglander.repository.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetWithSourceDO;

class ImageAssetEnrichedMapperIntegrationTest extends BaseTest {

    @Autowired private ImageAssetMapper assetMapper;
    @Autowired private ImageAssetSourceMapper sourceMapper;

    @Test
    void enrichedPageAndDetail_shouldReturnAssetAndSourceInOneProjection() {
        String suffix = Long.toHexString(System.nanoTime());
        ImageAssetDO asset = asset("img_" + suffix, "owner-" + suffix);
        assetMapper.insert(asset);
        ImageAssetSourceDO source = source(asset.getAssetId(), "device-" + suffix + ":channel-1");
        sourceMapper.insert(source);

        Page<ImageAssetWithSourceDO> page = assetMapper.selectEnrichedPageByCondition(
            new Page<ImageAssetWithSourceDO>(1, 24), "USER", "owner-" + suffix, null, null, null,
            "CAMERA_CAPTURE", null, null, "device-" + suffix, "channel-1", null, null);
        ImageAssetWithSourceDO detail = assetMapper.selectEnrichedByAssetId(asset.getAssetId());

        assertEquals(1L, page.getTotal());
        assertEquals(asset.getAssetId(), page.getRecords().get(0).getAsset().getAssetId());
        assertEquals(source.getSourceEntityId(), page.getRecords().get(0).getSource().getSourceEntityId());
        assertNotNull(detail);
        assertEquals(source.getOriginalFilename(), detail.getSource().getOriginalFilename());
    }

    private static ImageAssetDO asset(String assetId, String ownerId) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        ImageAssetDO asset = new ImageAssetDO();
        asset.setCreateTime(now);
        asset.setUpdateTime(now);
        asset.setAssetId(assetId);
        asset.setAssetName(assetId + ".jpg");
        asset.setStatus("AVAILABLE");
        asset.setStorageProvider("LOCAL");
        asset.setStorageKey("images/" + assetId + ".jpg");
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
        asset.setOwnerId(ownerId);
        asset.setRetentionPolicy("PERMANENT");
        asset.setVersion(0);
        return asset;
    }

    private static ImageAssetSourceDO source(String assetId, String entityId) {
        ImageAssetSourceDO source = new ImageAssetSourceDO();
        source.setCreateTime(LocalDateTime.now().withNano(0));
        source.setAssetId(assetId);
        source.setSourceType("CAMERA_CAPTURE");
        source.setSourceSystem("GB28181");
        source.setSourceEntityType("CAMERA");
        source.setSourceEntityId(entityId);
        source.setOriginalFilename("captured.jpg");
        source.setSourceMetadata("{\"deviceId\":\"device-1\"}");
        return source;
    }
}
