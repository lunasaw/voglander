package io.github.lunasaw.voglander.web.api.task.vo;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

/** Task detail with optional active execution and explicit action capabilities. */
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessTaskDetailVO extends BusinessTaskVO {
    private static final long serialVersionUID = 1L;
    @Schema(description = "仅非终态最近执行；没有活动执行时省略")
    private BusinessTaskExecutionVO activeExecution;
    @Schema(description = "当前 Handler 声明且状态允许的控制能力")
    private List<String> capabilities;
}
