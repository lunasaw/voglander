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
    MEDIA_NODE_OPERATION_FAILED(600210, "媒体节点操作失败"),
    ZLM_UNAVAILABLE(600211, "ZLM服务不可用"),
    PTZ_COMMAND_INVALID(600212, "未知PTZ指令"),

    // GB28181 专属支链命令异常（600213-600215）
    RECORD_QUERY_FAILED(600213, "录像查询失败"),
    ALARM_QUERY_FAILED(600214, "报警查询失败"),
    CONFIG_DOWNLOAD_FAILED(600215, "配置下载失败"),

    // 直播/回放域异常（700001-700005）
    LIVE_INVITE_TIMEOUT(700001, "直播拉流超时，设备未在规定时间内推流"),
    LIVE_NODE_UNAVAILABLE(700002, "无可用媒体节点"),
    STREAM_NOT_READY(700003, "流尚未就绪"),
    LIVE_STREAM_NOT_FOUND(700004, "直播会话不存在"),
    PLAYBACK_CONTROL_FAILED(700005, "回放控制指令发送失败"),
    SSE_CONNECTION_LIMIT(700006, "实时事件连接数已达上限，请稍后重试"),

    // 图像资产与采集域异常（710000-710018）
    IMAGE_FILE_TYPE_UNSUPPORTED(710000, "不支持的图像格式"),
    IMAGE_FILE_TOO_LARGE(710001, "图像文件超过大小限制"),
    IMAGE_DECODE_FAILED(710002, "图像无法解码"),
    IMAGE_PIXEL_LIMIT_EXCEEDED(710003, "图像像素超过限制"),
    IMAGE_ASSET_NOT_FOUND(710004, "图像资产不存在"),
    IMAGE_ASSET_STATE_CONFLICT(710005, "图像资产状态不允许该操作"),
    IMAGE_STORAGE_WRITE_FAILED(710006, "图像存储写入失败"),
    IMAGE_STORAGE_READ_FAILED(710007, "图像存储读取失败"),
    IMAGE_STORAGE_DELETE_FAILED(710008, "图像存储删除失败"),
    IMAGE_COLLECTION_SCHEDULE_INVALID(710011, "图像采集计划不合法"),
    IMAGE_COLLECTION_LIMIT_EXCEEDED(710012, "图像采集计划超过限制"),
    IMAGE_CAMERA_NOT_FOUND(710014, "采集设备或通道不存在"),
    IMAGE_CAMERA_OFFLINE(710015, "采集设备或通道当前离线"),
    IMAGE_STREAM_ESTABLISH_TIMEOUT(710016, "采集直播建流超时"),
    IMAGE_SNAPSHOT_FAILED(710017, "媒体服务器截图失败"),
    IMAGE_PERMISSION_DENIED(710018, "没有图像资源操作权限"),

    // 通用业务任务域异常（720000-720012）
    TASK_NOT_FOUND(720000, "业务任务不存在"),
    TASK_EXECUTION_NOT_FOUND(720001, "业务任务执行不存在"),
    TASK_TYPE_UNREGISTERED(720002, "业务任务类型未注册"),
    TASK_STATE_CONFLICT(720003, "业务任务状态不允许该操作"),
    TASK_SCHEDULE_INVALID(720004, "业务任务计划不合法"),
    TASK_LIMIT_EXCEEDED(720005, "业务任务超过限制"),
    TASK_PERMISSION_DENIED(720006, "没有业务任务操作权限"),
    TASK_PAYLOAD_INVALID(720007, "业务任务载荷不合法"),
    TASK_CLAIM_CONFLICT(720008, "业务任务执行已被其他节点领取"),
    TASK_LEASE_EXPIRED(720009, "业务任务执行租约已过期"),
    TASK_PROGRESS_INVALID(720010, "业务任务进度不合法"),
    TASK_RETRY_NOT_ALLOWED(720011, "业务任务当前不可重试"),
    TASK_HANDLER_FAILED(720012, "业务任务处理失败"),

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
