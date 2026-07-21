package io.github.lunasaw.voglander.manager.domaon.dto.image;

import java.time.LocalDateTime;

import lombok.Data;

/** Domain representation of an image asset. Storage internals stay manager-side. */
@Data
public class ImageAssetDTO {
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
