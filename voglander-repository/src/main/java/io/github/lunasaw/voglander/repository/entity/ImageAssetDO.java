package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_image_asset")
public class ImageAssetDO implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String assetId;
    private String assetName;
    private String status;
    private String storageProvider;
    private String storageBucket;
    private String storageKey;
    private String storageNodeId;
    private String contentType;
    private String imageFormat;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String checksumAlgorithm;
    private String checksum;
    private LocalDateTime capturedAt;
    private LocalDateTime ingestedAt;
    private String ownerType;
    private String ownerId;
    private String organizationId;
    private String idempotencyKey;
    private String retentionPolicy;
    private LocalDateTime expiresAt;
    private LocalDateTime deletedAt;
    private String deleteReason;
    private String failureCode;
    private String failureMessage;
    private Integer version;
}
