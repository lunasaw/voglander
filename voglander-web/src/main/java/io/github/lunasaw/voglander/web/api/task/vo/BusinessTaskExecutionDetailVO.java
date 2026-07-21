package io.github.lunasaw.voglander.web.api.task.vo;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Execution detail with its sanitized append-only event timeline. */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessTaskExecutionDetailVO extends BusinessTaskExecutionVO {
    private static final long serialVersionUID = 1L;
    private List<BusinessTaskEventVO> events;
}
