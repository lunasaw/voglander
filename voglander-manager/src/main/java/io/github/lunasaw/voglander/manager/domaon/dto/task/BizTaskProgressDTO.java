package io.github.lunasaw.voglander.manager.domaon.dto.task;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

/** Monotonic progress update passed from TaskContext to the Manager boundary. */
@Data
public class BizTaskProgressDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String taskId;
    private String executionId;
    private String claimToken;
    private Long current;
    private Long total;
    private String message;
    private Long revision;
    private Boolean forcePersist;
    private LocalDateTime reportedAt;
}
