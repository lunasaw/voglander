package io.github.lunasaw.voglander.web.api.image.vo;

import lombok.Data;

/** Safe enriched collection view; payload and lease/claim fields are intentionally absent. */
@Data
public class ImageCollectionVO {
    private String taskId;
    private String taskName;
    private String taskMode;
    private String state;
    private Long scheduleStartTime;
    private Long scheduleEndTime;
    private Long intervalSeconds;
    private Long nextPlanTime;
    private Integer plannedCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer missedCount;
    private Long progressCurrent;
    private Long progressTotal;
    private String lastFailureCode;
    private String lastFailureMessage;
    private String deviceId;
    private String channelId;
    private String deviceName;
    private String channelName;
    private String retentionPolicy;
    private Long createTime;
    private Long updateTime;
}
