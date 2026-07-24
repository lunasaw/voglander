package io.github.lunasaw.voglander.web.api.image.vo;

import lombok.Data;

@Data
public class ImageAssetSourceVO {
    private String sourceType;
    private String sourceSystem;
    private String sourceEntityType;
    private String sourceEntityId;
    private String sourceTaskId;
    private String sourceExecutionId;
    private String originalFilename;
    private ImageAssetSourceMetadataVO sourceMetadata;
}
