package io.github.lunasaw.voglander.common.exception;

/**
 * @author luna
 * @date 2024/1/24
 */
public enum ServiceExceptionEnum {
    PARAM_ERROR(600001, "参数错误"),
    FREQUENT_ERROR(600002, "提交过于频繁，请先检查下是否已经已经成功"),
    CLICK_FREQUENT_ERROR(600003, "提交过于频繁，请稍后重试"),
    BIZ_KEY_ALREADY_PROCESSED_ERROR(600004, "重复处理"),

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
