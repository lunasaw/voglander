package io.github.lunasaw.voglander.web.api.zlm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 推流代理视图对象
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "推流代理视图对象")
public class PushProxyVO {

    @Schema(description = "推流代理ID", example = "1")
    private Long              id;

    @Schema(description = "创建时间戳(毫秒)", example = "1640995200000")
    private Long              createTime;

    @Schema(description = "更新时间戳(毫秒)", example = "1640995200000")
    private Long              updateTime;

    @Schema(description = "应用名称", example = "live")
    private String            app;

    @Schema(description = "流名称", example = "test")
    private String            stream;

    @Schema(description = "推流目标地址", example = "rtmp://push.example.com/live/test")
    private String            dstUrl;

    @Schema(description = "推流协议", example = "rtmp")
    private String            schema;

    @Schema(description = "代理状态", example = "1", allowableValues = {"0", "1"})
    private Integer           status;

    @Schema(description = "在线状态", example = "1", allowableValues = {"0", "1"})
    private Integer           onlineStatus;

    @Schema(description = "代理密钥", example = "push_proxy_key")
    private String            proxyKey;

    @Schema(description = "节点ID", example = "zlm-node-1")
    private String            serverId;

    @Schema(description = "是否启用", example = "1", allowableValues = {"0", "1"})
    private Integer           enabled;

    @Schema(description = "代理描述", example = "测试推流代理")
    private String            description;

    @Schema(description = "扩展字段JSON", example = "{\"vhost\":\"__defaultVhost__\",\"retryCount\":-1}")
    private String            extend;

    @Schema(description = "ZLM推流扩展参数")
    private PushProxyExtendVO extendObj;

    /**
     * 推流代理扩展参数视图对象
     */
    @Data
    @Schema(description = "推流代理扩展参数视图对象")
    public static class PushProxyExtendVO {

        @Schema(description = "虚拟主机", example = "__defaultVhost__")
        private String  vhost;

        @Schema(description = "推流重试次数，-1表示无限重试", example = "-1")
        private Integer retryCount;

        @Schema(description = "rtsp推流方式，0:tcp，1:udp", example = "0")
        private Integer rtpType;

        @Schema(description = "推流超时时间，单位秒", example = "10")
        private Integer timeoutSec;

        @Schema(description = "是否启用自动重连", example = "true")
        private Boolean autoReconnect;

        @Schema(description = "推流失败重试间隔，单位秒", example = "5")
        private Integer retryInterval;

        @Schema(description = "是否启用推流状态监控", example = "true")
        private Boolean enableMonitor;

        @Schema(description = "推流质量监控阈值", example = "0.8")
        private Double  qualityThreshold;

        @Schema(description = "最大推流码率，单位kbps", example = "5000")
        private Integer maxBitrate;

        @Schema(description = "最小推流码率，单位kbps", example = "500")
        private Integer minBitrate;

        @Schema(description = "是否启用推流认证", example = "false")
        private Boolean enableAuth;

        @Schema(description = "推流认证用户名", example = "user")
        private String  authUser;

        @Schema(description = "推流认证密码", example = "password")
        private String  authPassword;

        @Schema(description = "无人观看是否自动停止推流", example = "false")
        private Boolean autoStop;

        @Schema(description = "自动停止延迟时间，单位秒", example = "300")
        private Integer autoStopDelay;

        @Schema(description = "推流优先级，数值越大优先级越高", example = "1")
        private Integer priority;

        @Schema(description = "是否启用推流加密", example = "false")
        private Boolean enableEncrypt;

        @Schema(description = "推流加密密钥", example = "secret_key")
        private String  encryptKey;
    }
}