package io.github.lunasaw.voglander.web.api.image.req;

import java.time.Instant;

import lombok.Data;

@Data
public class ImageAssetQueryReq {
    private String assetId;
    private String assetName;
    private String status;
    private String sourceType;
    private String sourceTaskId;
    private String sourceExecutionId;
    private String deviceId;
    private String channelId;
    private Long capturedStart;
    private Long capturedEnd;

    public java.time.LocalDateTime capturedStartTime() { return capturedStart == null ? null : java.time.LocalDateTime.ofInstant(Instant.ofEpochMilli(capturedStart), java.time.ZoneId.systemDefault()); }
    public java.time.LocalDateTime capturedEndTime() { return capturedEnd == null ? null : java.time.LocalDateTime.ofInstant(Instant.ofEpochMilli(capturedEnd), java.time.ZoneId.systemDefault()); }
}
