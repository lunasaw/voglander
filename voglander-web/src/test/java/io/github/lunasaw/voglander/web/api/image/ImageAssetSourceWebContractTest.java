package io.github.lunasaw.voglander.web.api.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.web.api.image.assembler.ImageAssetWebAssembler;
import io.github.lunasaw.voglander.web.api.image.vo.ImageAssetVO;

class ImageAssetSourceWebContractTest {

    @Test
    void nestedSourceReturnsOnlyExplicitMetadataAllowlist() {
        ImageAssetDTO asset = new ImageAssetDTO();
        asset.setAssetId("img_1");
        ImageAssetSourceDTO source = new ImageAssetSourceDTO();
        source.setSourceType("CAMERA_CAPTURE");
        source.setSourceSystem("VOGLANDER_CAPTURE");
        source.setSourceEntityType("CAMERA");
        source.setSourceEntityId("device-1:channel-1");
        JSONObject metadata = new JSONObject();
        metadata.put("deviceId", "device-1");
        metadata.put("channelId", "channel-1");
        metadata.put("deviceName", "North Gate");
        metadata.put("channelName", "Entry");
        metadata.put("protocol", "RTSP");
        metadata.put("nodeServerId", "secret-node");
        metadata.put("streamId", "secret-stream");
        metadata.put("storageKey", "private/key");
        source.setSourceMetadata(metadata);

        ImageAssetVO result = new ImageAssetWebAssembler().toVO(asset, source);
        String json = JSON.toJSONString(result);

        assertEquals("device-1", result.getSource().getSourceMetadata().getDeviceId());
        assertEquals("Entry", result.getSource().getSourceMetadata().getChannelName());
        assertFalse(json.contains("protocol"));
        assertFalse(json.contains("secret-node"));
        assertFalse(json.contains("secret-stream"));
        assertFalse(json.contains("private/key"));
    }
}
