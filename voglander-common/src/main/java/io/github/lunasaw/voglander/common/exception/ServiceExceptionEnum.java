package io.github.lunasaw.voglander.common.exception;

/**
 * @author luna
 * @date 2024/1/24
 */
public enum ServiceExceptionEnum {
    UNKNOWN(600000, "未知异常"),
    PARAM_ERROR(600001, "参数错误"),
    FREQUENT_ERROR(600002, "提交过于频繁，请先检查下是否已经已经成功"),
    CLICK_FREQUENT_ERROR(600003, "提交过于频繁，请稍后重试"),
    BIZ_KEY_ALREADY_PROCESSED_ERROR(600004, "重复处理"),
    BUSINESS_EXCEPTION(600005, "业务异常"),
    DATA_NOT_EXISTS(600006, "数据不存在"),

    // 用户相关错误
    USER_NOT_FOUND(600101, "用户不存在"),
    PASSWORD_ERROR(600102, "密码错误"),
    USER_DISABLED(600103, "用户已被禁用"),
    TOKEN_INVALID(600104, "token无效"),
    TOKEN_EXPIRED(600105, "token已过期"),
    LOGIN_REQUIRED(600106, "请先登录"),

    // 业务域异常
    DEVICE_NOT_FOUND(600201, "设备不存在"),
    DEVICE_OPERATION_FAILED(600202, "设备操作失败"),
    CHANNEL_OPERATION_FAILED(600203, "通道操作失败"),
    STREAM_PROXY_NOT_FOUND(600204, "流代理不存在"),
    STREAM_PROXY_OPERATION_FAILED(600205, "流代理操作失败"),
    PUSH_PROXY_NOT_FOUND(600206, "推流代理不存在"),
    PUSH_PROXY_OPERATION_FAILED(600207, "推流代理操作失败"),
    MEDIA_SESSION_OPERATION_FAILED(600208, "媒体会话操作失败"),
    EXPORT_TASK_OPERATION_FAILED(600209, "导出任务操作失败"),
    MEDIA_NODE_OPERATION_FAILED(600210, "媒体节点操作失败"),
    ZLM_UNAVAILABLE(600211, "ZLM服务不可用"),
    PTZ_COMMAND_INVALID(600212, "未知PTZ指令"),

    // 直播/回放域异常（700001-700005）
    LIVE_INVITE_TIMEOUT(700001, "直播拉流超时，设备未在规定时间内推流"),
    LIVE_NODE_UNAVAILABLE(700002, "无可用媒体节点"),
    STREAM_NOT_READY(700003, "流尚未就绪"),
    LIVE_STREAM_NOT_FOUND(700004, "直播会话不存在"),
    PLAYBACK_CONTROL_FAILED(700005, "回放控制指令发送失败"),
    SSE_CONNECTION_LIMIT(700006, "实时事件连接数已达上限，请稍后重试"),

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
