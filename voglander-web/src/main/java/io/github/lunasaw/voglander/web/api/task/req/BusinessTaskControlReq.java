package io.github.lunasaw.voglander.web.api.task.req;

import java.io.Serializable;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/** Click-time task control command body. */
@Data
public class BusinessTaskControlReq implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "必填的任务乐观锁版本；缺失或过期均拒绝控制", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer expectedVersion;
    private String executionId;
    private String idempotencyKey;
    private String reason;
}
