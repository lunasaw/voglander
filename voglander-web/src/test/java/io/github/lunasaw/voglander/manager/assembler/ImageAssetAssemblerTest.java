package io.github.lunasaw.voglander.manager.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;

class ImageAssetAssemblerTest {
    @Test
    void assetRoundTrip_shouldPreserveAllMetadataFields() {
        ImageAssetDTO source = new ImageAssetDTO();
        source.setAssetId("img_1");
        source.setAssetName("photo.jpg");
        source.setStorageKey("images/2026/07/15/img_1.jpg");
        source.setFileSize(42L);
        source.setWidth(7);
        source.setHeight(6);
        source.setCapturedAt(LocalDateTime.of(2026, 7, 15, 1, 2));
        source.setVersion(3);

        ImageAssetDO restored = ImageAssetAssembler.toDO(source);

        assertEquals(source.getAssetId(), restored.getAssetId());
        assertEquals(source.getStorageKey(), restored.getStorageKey());
        assertEquals(source.getFileSize(), restored.getFileSize());
        assertEquals(source.getCapturedAt(), restored.getCapturedAt());
        assertEquals(source.getVersion(), restored.getVersion());
    }

    @Test
    void sourceAndConfigRoundTrip_shouldSerializeJsonFieldsWithFastjson2() {
        ImageAssetSourceDTO source = new ImageAssetSourceDTO();
        source.setAssetId("img_1");
        JSONObject metadata = new JSONObject();
        metadata.put("deviceId", "d1");
        metadata.put("secret", "must-not-be-added-by-assembler");
        source.setSourceMetadata(metadata);
        ImageAssetSourceDO sourceDO = ImageAssetAssembler.toDO(source);
        assertEquals("d1", ImageAssetAssembler.toDTO(sourceDO).getSourceMetadata().getString("deviceId"));

        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId("btask_1");
        config.setDeviceId("d1");
        config.setChannelId("c1");
        JSONObject options = new JSONObject();
        options.put("quality", "high");
        config.setCaptureOptions(options);
        ImageCollectionConfigDO configDO = ImageAssetAssembler.toDO(config);
        assertEquals("high", ImageAssetAssembler.toDTO(configDO).getCaptureOptions().getString("quality"));
    }
}
