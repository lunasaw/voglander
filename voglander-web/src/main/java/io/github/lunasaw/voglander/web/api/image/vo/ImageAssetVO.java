package io.github.lunasaw.voglander.web.api.image.vo;

import lombok.Data;

@Data
public class ImageAssetVO {
    private String assetId;
    private String assetName;
    private String status;
    private String contentType;
    private String imageFormat;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Long capturedAt;
    private Long ingestedAt;
    private String checksum;
    private String sourceType;
    private String sourceTaskId;
    private String sourceExecutionId;
    private String sourceEntityId;
    private String originalFilename;
}
