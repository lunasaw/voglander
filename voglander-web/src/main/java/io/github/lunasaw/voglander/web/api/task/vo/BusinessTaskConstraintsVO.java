package io.github.lunasaw.voglander.web.api.task.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.Data;

/** Stable task-center state, type, capability and limit contract. */
@Data
public class BusinessTaskConstraintsVO implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> taskTypes;
    private List<String> taskModes;
    private List<String> taskStates;
    private List<String> executionStates;
    private Map<String, List<String>> capabilities;
    private Integer maxPlannedCount;
    private Integer maxScheduleDurationDays;
    private Integer maxPayloadBytes;
}
