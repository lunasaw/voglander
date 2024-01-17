package io.github.lunasaw.voglander.intergration.wrapper.excel.exception;

import lombok.Getter;

/**
 * @author luna
 * @date 2024/1/17
 */
@Getter
public enum ExcelExceptionEnums {

    GET_HEAD_EXCEPTION(10000001, "组装表头发生异常"),
    EXCEL_FILE_PATH_ISNULL(10000002, "暂存excel的文件路径为空"),
    EXCEL_RECORDS_ISNULL(10000003, "excel的文件内容不能为空"),
    TABLE_HEAD_ISNULL(10000004, "表头不能为空"),
    TABLE_WITH_ISNULL(10000005, "表头宽度异常"),
    SHEET_INFO_ISNULL(10000006, "sheet信息为空"),
    EXCEL_WRITER_ISNULL(10000007, "ExcelWiter信息为空"),
    EXCEL_READ_EXCEPTION(10000008, "excel读文件发生异常"),
    HEAD_DIFF_FOR_READ_EXCEPTION(10000009, "表头的存在差异"),
    HEAD_MATCH_EXCEPTION(10000010, "表头的匹配结果为空"),
    GEN_EXCEL_TEMP_EXCEPTION(10000011, "生成excel模板发生异常"),
    IMPORT_EXCEL_EXCEPTION(10000012, "导入excel发生异常"),
    GET_IMPORT_TEMP_DTO_EXCEPTION(10000013, "获取excel模板类异常"),
    ;

    private String desc;
    private int    code;

    ExcelExceptionEnums(int code, String desc) {
        this.desc = desc;
        this.code = code;
    }

    public String toValue() {
        return String.format("[%s]-%s", getCode(), getDesc());
    }
}
