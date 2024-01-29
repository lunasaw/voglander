package io.github.lunasaw.voglander.common.enums.export;

import lombok.Getter;

/**
 * @author weidian
 */

@Getter
public enum ExportTaskStatusEnum {
    PROCESSING(0, "处理中"),
    FINISHED(1, "已完成"),
    ERROR(-1, "出错");

    private int    code;
    private String desc;

    ExportTaskStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
