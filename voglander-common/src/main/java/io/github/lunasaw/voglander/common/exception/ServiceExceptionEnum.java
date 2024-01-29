package io.github.lunasaw.voglander.common.exception;

/**
 * @author luna
 * @date 2024/1/24
 */
public enum ServiceExceptionEnum {
    /**
     * 系统通用
     */
    SYSTEM_ERROR(1000, "系统开小差了，请重新尝试"),
    PARAM_ERROR(1001, "参数错误"),
    FREQUENT_ERROR(1002, "提交过于频繁，请先检查下是否已经已经成功"),
    CLICK_FREQUENT_ERROR(1003, "提交过于频繁，请稍后重试"),
    BIZ_KEY_ALREADY_PROCESSED_ERROR(1010, "重复处理"),

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

    ServiceExceptionEnum(int code, String message) {
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
