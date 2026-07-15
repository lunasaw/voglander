package io.github.lunasaw.voglander.web.api.task.req;

import java.io.Serializable;

import lombok.Data;

/** Click-time task control command body. */
@Data
public class BusinessTaskControlReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer expectedVersion;
    private String executionId;
    private String idempotencyKey;
    private String reason;
}
