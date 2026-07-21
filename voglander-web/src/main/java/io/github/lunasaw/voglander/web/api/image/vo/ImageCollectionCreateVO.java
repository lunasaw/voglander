package io.github.lunasaw.voglander.web.api.image.vo;

import lombok.Data;

@Data
public class ImageCollectionCreateVO {
    private String taskId;
    private String executionId;
    private String state;
    private Integer plannedCount;
    private Long nextPlanTime;
}
