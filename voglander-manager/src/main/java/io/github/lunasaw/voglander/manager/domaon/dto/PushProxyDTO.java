package io.github.lunasaw.voglander.manager.domaon.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 推流代理数据传输对象
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
public class PushProxyDTO {

    /**
     * 主键ID
     */
    private Long          id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 应用名称
     */
    private String        app;

    /**
     * 流ID
     */
    private String        stream;

    /**
     * 推流目标地址
     */
    private String        dstUrl;

    /**
     * 推流协议 rtmp/rtsp
     */
    private String        schema;

    /**
     * 代理状态 1启用 0禁用
     */
    private Integer       status;

    /**
     * 流在线状态 1在线 0离线
     */
    private Integer       onlineStatus;

    /**
     * ZLM返回的代理key
     */
    private String        proxyKey;

    /**
     * 节点ID，保存当前添加推流代理的节点
     */
    private String        serverId;

    /**
     * 是否启用 1启用 0禁用
     */
    private Integer       enabled;

    /**
     * 代理描述
     */
    private String        description;

    /**
     * 扩展字段
     */
    private String        extend;

    private ExtendObj     extendObj;

    @Data
    public static class ExtendObj {

        /**
         * ZLM扩展参数对象 - 虚拟主机（用于扩展配置）
         */
        private String  vhost;

        /**
         * 推流重试次数，默认为-1无限重试
         */
        private Integer retryCount;

        /**
         * rtsp推流时，推流方式，0：tcp，1：udp
         */
        private Integer rtpType;

        /**
         * 推流超时时间，单位秒
         */
        private Integer timeoutSec;

        /**
         * 是否启用自动重连
         */
        private Boolean autoReconnect;

        /**
         * 推流失败重试间隔，单位秒
         */
        private Integer retryInterval;

        /**
         * 是否启用推流状态监控
         */
        private Boolean enableMonitor;

        /**
         * 推流质量监控阈值
         */
        private Double  qualityThreshold;

        /**
         * 最大推流码率，单位kbps
         */
        private Integer maxBitrate;

        /**
         * 最小推流码率，单位kbps
         */
        private Integer minBitrate;

        /**
         * 是否启用推流认证
         */
        private Boolean enableAuth;

        /**
         * 推流认证用户名
         */
        private String  authUser;

        /**
         * 推流认证密码
         */
        private String  authPassword;

        /**
         * 无人观看是否自动停止推流
         */
        private Boolean autoStop;

        /**
         * 自动停止延迟时间，单位秒
         */
        private Integer autoStopDelay;

        /**
         * 推流优先级（数值越大优先级越高）
         */
        private Integer priority;

        /**
         * 是否启用推流加密
         */
        private Boolean enableEncrypt;

        /**
         * 推流加密密钥
         */
        private String  encryptKey;
    }
}