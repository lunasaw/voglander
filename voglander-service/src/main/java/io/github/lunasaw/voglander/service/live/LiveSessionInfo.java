package io.github.lunasaw.voglander.service.live;

import lombok.Data;

/**
 * 直播/回放会话信息值对象。
 * <p>
 * 存于 {@link LiveStreamRegistry}（Redis 主库），作为 streamId → 会话元数据的权威快照，
 * 供多路复用判定、播放地址秒取、跨节点亲和路由使用。序列化用 FastJSON2。
 * </p>
 *
 * @author luna
 */
@Data
public class LiveSessionInfo {

    /**
     * SIP Call-ID（INVITE 成功后回填）。
     */
    private String  callId;

    /**
     * 媒体节点 serverId（亲和路由）。
     */
    private String  nodeServerId;

    /**
     * SDP 中下发给设备的媒体接收 IP。
     */
    private String  sdpIp;

    /**
     * ZLM 分配的 RTP 接收端口。
     */
    private Integer rtpPort;

    /**
     * 会话状态，取值见 {@code MediaSessionConstant.Status}。
     */
    private Integer status;

    /**
     * 会话类型，取值见 {@code MediaSessionConstant.Type}。
     */
    private String  sessionType;

    /**
     * 创建时间（Unix 毫秒）。
     */
    private long    createMs;

    /**
     * FastJSON2 序列化后的 PlayUrl（ZLM 多协议播放地址）。
     */
    private String  playUrlsJson;
}
