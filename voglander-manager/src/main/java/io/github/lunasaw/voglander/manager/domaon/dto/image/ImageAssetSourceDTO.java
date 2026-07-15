package io.github.lunasaw.voglander.manager.domaon.dto.image;

import java.time.LocalDateTime;

import lombok.Data;

import com.alibaba.fastjson2.JSONObject;

@Data
public class ImageAssetSourceDTO {
    private Long id;
    private LocalDateTime createTime;
    private String assetId;
    private String sourceType;
    private String sourceSystem;
    private String sourceEntityType;
    private String sourceEntityId;
    private String sourceTaskId;
    private String sourceExecutionId;
    private String originalFilename;
    private JSONObject sourceMetadata;
}
