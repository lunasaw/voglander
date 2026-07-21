package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_biz_task_execution")
public class BizTaskExecutionDO implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String executionId;
    private String taskId;
    private Integer scheduleVersion;
    private LocalDateTime plannedAt;
    private LocalDateTime deadlineAt;
    private String state;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime nextAttemptTime;
    private LocalDateTime startedAt;
    private LocalDateTime heartbeatAt;
    private LocalDateTime finishedAt;
    private String claimToken;
    private String workerNode;
    private LocalDateTime leaseUntil;
    private Long progressCurrent;
    private Long progressTotal;
    private String progressMessage;
    private Long progressRevision;
    private String resultRefType;
    private String resultRefId;
    private String resultSummary;
    private String failureCode;
    private String failureMessage;
    private Boolean retryable;
    private String retryOriginExecutionId;
    private Integer version;
}
