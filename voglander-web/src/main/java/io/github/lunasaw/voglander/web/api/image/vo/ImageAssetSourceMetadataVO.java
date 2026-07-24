package io.github.lunasaw.voglander.web.api.image.vo;

import lombok.Data;

@Data
public class ImageAssetSourceMetadataVO {
    private String deviceId;
    private String channelId;
    private String deviceName;
    private String channelName;
}
