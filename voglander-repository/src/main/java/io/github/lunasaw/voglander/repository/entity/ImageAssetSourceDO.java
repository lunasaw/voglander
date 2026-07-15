package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_image_asset_source")
public class ImageAssetSourceDO implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
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
    private String sourceMetadata;
}
