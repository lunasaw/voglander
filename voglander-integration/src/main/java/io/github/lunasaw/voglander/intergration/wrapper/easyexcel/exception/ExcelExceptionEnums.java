package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.exception;

import lombok.Getter;

/**
 * @author luna
 * @date 2024/1/17
 */
@Getter
public enum ExcelExceptionEnums {

    /**
     * excel
     */
    EXCEL_FILE_PATH_ISNULL(10000002, "暂存Excel的文件路径为空"),
    EXCEL_RECORDS_ISNULL(10000003, "Excel的文件内容不能为空"),
    TABLE_HEAD_ISNULL(10000004, "表头不能为空"),
    EXCEL_READ_EXCEPTION(10000008, "Excel读文件发生异常"),
    HEAD_MATCH_EXCEPTION(10000010, "表头的匹配结果为空"),
    GEN_EXCEL_TEMP_EXCEPTION(10000011, "生成Excel模板发生异常"),
    IMPORT_EXCEL_EXCEPTION(10000012, "导入Excel发生异常"),
    ;

    private int    code;
    private String message;

    ExcelExceptionEnums(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
