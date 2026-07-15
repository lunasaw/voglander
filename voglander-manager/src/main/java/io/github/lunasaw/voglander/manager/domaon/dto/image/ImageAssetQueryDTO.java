package io.github.lunasaw.voglander.manager.domaon.dto.image;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ImageAssetQueryDTO {
    private String ownerType;
    private String ownerId;
    private String assetId;
    private String assetName;
    private String status;
    private String sourceType;
    private String sourceTaskId;
    private String sourceExecutionId;
    private String deviceId;
    private String channelId;
    private LocalDateTime capturedStart;
    private LocalDateTime capturedEnd;
}
