package io.github.lunasaw.voglander.manager.domaon.dto.image;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import lombok.Data;

/** Generic task facts combined with the immutable image camera configuration. */
@Data
public class ImageCollectionEnrichedDTO {
    private BizTaskDTO task;
    private ImageCollectionConfigDTO config;
}
