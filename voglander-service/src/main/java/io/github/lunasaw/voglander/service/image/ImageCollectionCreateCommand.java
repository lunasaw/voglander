package io.github.lunasaw.voglander.service.image;

import java.time.LocalDateTime;

/** Trusted domain command for creating one camera image collection task. */
public final class ImageCollectionCreateCommand {
    private final String taskName;
    private final String collectionMode;
    private final String deviceId;
    private final String channelId;
    private final LocalDateTime scheduleStartTime;
    private final LocalDateTime scheduleEndTime;
    private final Long intervalSeconds;
    private final String retentionPolicy;
    private final String ownerType;
    private final String ownerId;
    private final String organizationId;
    private final String idempotencyKey;

    public ImageCollectionCreateCommand(String taskName, String collectionMode, String deviceId, String channelId,
        LocalDateTime scheduleStartTime, LocalDateTime scheduleEndTime, Long intervalSeconds,
        String retentionPolicy, String ownerType, String ownerId, String organizationId, String idempotencyKey) {
        this.taskName = taskName;
        this.collectionMode = collectionMode;
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.scheduleStartTime = scheduleStartTime;
        this.scheduleEndTime = scheduleEndTime;
        this.intervalSeconds = intervalSeconds;
        this.retentionPolicy = retentionPolicy;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.organizationId = organizationId;
        this.idempotencyKey = idempotencyKey;
    }

    public String taskName() { return taskName; }
    public String collectionMode() { return collectionMode; }
    public String deviceId() { return deviceId; }
    public String channelId() { return channelId; }
    public LocalDateTime scheduleStartTime() { return scheduleStartTime; }
    public LocalDateTime scheduleEndTime() { return scheduleEndTime; }
    public Long intervalSeconds() { return intervalSeconds; }
    public String retentionPolicy() { return retentionPolicy; }
    public String ownerType() { return ownerType; }
    public String ownerId() { return ownerId; }
    public String organizationId() { return organizationId; }
    public String idempotencyKey() { return idempotencyKey; }
}
