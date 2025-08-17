package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 推流代理ZLM扩展参数请求
 * <p>
 * 包含ZLM推流代理的扩展配置参数，支持高级推流控制
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "推流代理ZLM扩展参数")
public class PushProxyExtendReq {

    @Schema(description = "虚拟主机", example = "__defaultVhost__", defaultValue = "__defaultVhost__")
    private String  vhost;

    @Schema(description = "推流重试次数，-1表示无限重试", example = "-1", defaultValue = "-1")
    private Integer retryCount;

    @Schema(description = "rtsp推流方式，0:tcp，1:udp", example = "0", allowableValues = {"0", "1"}, defaultValue = "0")
    private Integer rtpType;

    @Schema(description = "推流超时时间，单位秒", example = "10", defaultValue = "10")
    private Integer timeoutSec;

    @Schema(description = "是否启用自动重连", example = "true", defaultValue = "false")
    private Boolean autoReconnect;

    @Schema(description = "推流失败重试间隔，单位秒", example = "5", defaultValue = "5")
    private Integer retryInterval;

    @Schema(description = "是否启用推流状态监控", example = "true", defaultValue = "false")
    private Boolean enableMonitor;

    @Schema(description = "推流质量监控阈值", example = "0.8", defaultValue = "0.8")
    private Double  qualityThreshold;

    @Schema(description = "最大推流码率，单位kbps", example = "5000")
    private Integer maxBitrate;

    @Schema(description = "最小推流码率，单位kbps", example = "500")
    private Integer minBitrate;

    @Schema(description = "是否启用推流认证", example = "false", defaultValue = "false")
    private Boolean enableAuth;

    @Schema(description = "推流认证用户名", example = "user")
    private String  authUser;

    @Schema(description = "推流认证密码", example = "password")
    private String  authPassword;

    @Schema(description = "无人观看是否自动停止推流", example = "false", defaultValue = "false")
    private Boolean autoStop;

    @Schema(description = "自动停止延迟时间，单位秒", example = "300", defaultValue = "300")
    private Integer autoStopDelay;

    @Schema(description = "推流优先级，数值越大优先级越高", example = "1", defaultValue = "1")
    private Integer priority;

    @Schema(description = "是否启用推流加密", example = "false", defaultValue = "false")
    private Boolean enableEncrypt;

    @Schema(description = "推流加密密钥", example = "secret_key")
    private String  encryptKey;
}