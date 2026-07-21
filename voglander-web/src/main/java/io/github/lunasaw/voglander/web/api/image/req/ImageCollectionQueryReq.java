package io.github.lunasaw.voglander.web.api.image.req;

import lombok.Data;

@Data
public class ImageCollectionQueryReq {
    private String taskName;
    private String collectionMode;
    private String state;
    private String deviceId;
    private String channelId;
}
