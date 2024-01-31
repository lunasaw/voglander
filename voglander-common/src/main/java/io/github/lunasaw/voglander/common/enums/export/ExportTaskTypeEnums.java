package io.github.lunasaw.voglander.common.enums.export;

import lombok.Getter;

/**
 * @author luna
 * @date 2024/1/29
 */
@Getter
public enum ExportTaskTypeEnums {
    DEVICE_LIST(1)

    ;

    private Integer value;

    private ExportTaskTypeEnums(Integer value) {
        this.value = value;
    }

}
