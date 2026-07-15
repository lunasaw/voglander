package io.github.lunasaw.voglander.web.api.task.vo;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Task detail with optional active execution and explicit action capabilities. */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessTaskDetailVO extends BusinessTaskVO {
    private static final long serialVersionUID = 1L;
    private BusinessTaskExecutionVO activeExecution;
    private List<String> capabilities;
}
