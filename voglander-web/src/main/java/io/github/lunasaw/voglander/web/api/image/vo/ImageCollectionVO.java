package io.github.lunasaw.voglander.web.api.image.vo;

import java.util.List;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/** Safe enriched collection view; payload and lease/claim fields are intentionally absent. */
@Data
public class ImageCollectionVO {
    private String taskId;
    @Schema(description = "任务行乐观锁版本")
    private Integer version;
    @Schema(description = "调度版本，不作为控制 expectedVersion")
    private Integer scheduleVersion;
    @Schema(description = "当前状态允许的控制能力")
    private List<String> capabilities;
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
    private Integer cancelledCount;
    private Long progressCurrent;
    private Long progressTotal;
    private String progressMessage;
    private Long progressRevision;
    @Schema(description = "最近一次执行 ID")
    private String lastExecutionId;
    @Schema(description = "结果引用类型")
    private String resultRefType;
    @Schema(description = "结果引用 ID")
    private String resultRefId;
    @Schema(description = "安全结果摘要")
    private String resultSummary;
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
