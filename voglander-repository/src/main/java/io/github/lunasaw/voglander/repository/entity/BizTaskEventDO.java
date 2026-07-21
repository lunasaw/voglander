package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("tb_biz_task_event")
public class BizTaskEventDO implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDateTime createTime;
    private String eventId;
    private String taskId;
    private String executionId;
    private String eventType;
    private String fromState;
    private String toState;
    private Integer attemptNo;
    private Long progressCurrent;
    private Long progressTotal;
    private String progressMessage;
    private String failureCode;
    private String failureMessage;
    private String actorType;
    private String actorId;
    private String workerNode;
    private String traceId;
    private String dedupeKey;
    private String eventData;
    private LocalDateTime occurredAt;
}
