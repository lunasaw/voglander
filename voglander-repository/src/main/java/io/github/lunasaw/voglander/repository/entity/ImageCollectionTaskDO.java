package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;

import lombok.Data;

/** Read projection joining an image collection task with its immutable camera config. */
@Data
public class ImageCollectionTaskDO implements Serializable {
    private static final long serialVersionUID = 1L;
    private BizTaskDO task;
    private ImageCollectionConfigDO config;
}
