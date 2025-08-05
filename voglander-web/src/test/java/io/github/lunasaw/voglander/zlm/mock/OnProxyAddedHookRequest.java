package io.github.lunasaw.voglander.zlm.mock;

import lombok.Builder;
import lombok.Data;

/**
 * 拉流代理添加Hook请求参数
 *
 * @author luna
 * @date 2025-01-23
 */
@Data
@Builder
public class OnProxyAddedHookRequest {

    /**
     * 媒体服务器ID
     */
    private String  mediaServerId;

    /**
     * 应用名称
     */
    private String  app;

    /**
     * 流ID
     */
    private String  stream;

    /**
     * 拉流地址
     */
    private String  url;

    /**
     * 代理密钥
     */
    private String  key;

    /**
     * 虚拟主机
     */
    private String  vhost;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * RTP类型
     */
    private Integer rtpType;

    /**
     * 超时时间(秒)
     */
    private Integer timeoutSec;

    /**
     * 是否启用HLS
     */
    private Boolean enableHls;

    /**
     * 是否启用HLS FMP4
     */
    private Boolean enableHlsFmp4;

    /**
     * 是否启用MP4录制
     */
    private Boolean enableMp4;

    /**
     * 是否启用RTSP
     */
    private Boolean enableRtsp;

    /**
     * 是否启用RTMP
     */
    private Boolean enableRtmp;

    /**
     * 是否启用TS
     */
    private Boolean enableTs;

    /**
     * 是否启用FMP4
     */
    private Boolean enableFmp4;
}