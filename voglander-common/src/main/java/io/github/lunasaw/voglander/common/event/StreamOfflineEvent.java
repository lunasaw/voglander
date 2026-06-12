package io.github.lunasaw.voglander.common.event;

/**
 * ZLM 流下线事件。
 * <p>
 * 由 {@code VoglanderZlmHookServiceImpl} 发布（integration 层）：
 * {@code onStreamChanged(regist=false)} 常规下线（reason=stream_offline），
 * {@code onStreamNoneReader} 无人观看到点回收（reason=none_reader）。
 * 由 {@code LiveStreamEventListener} 消费（service 层），委托 {@code MediaPlayService.closeStream}
 * 做标准 BYE 收尾（closeRtpServer + sendBye + 标 CLOSED + 清缓存 + 推 SSE live.closed）。
 * 与 {@link StreamReadyEvent} 对称，置于 voglander-common 打破 service→integration 循环依赖。
 * </p>
 *
 * @author luna
 */
public class StreamOfflineEvent {

    /**
     * 下线原因默认值：onStreamChanged(regist=false) 触发的常规下线。
     */
    public static final String REASON_STREAM_OFFLINE = "stream_offline";

    /**
     * 流标识（ZLM stream id，即 streamId）。
     */
    private final String streamId;

    /**
     * ZLM mediaServerId，便于定位/日志，可为 null。
     */
    private final String serverId;

    /**
     * 下线来源，透传到 SSE {@code live.closed} 的 {@code reason}，便于前端与排障区分：
     * {@code stream_offline}（常规下线）/ {@code none_reader}（无人观看到点回收）。
     */
    private final String reason;

    public StreamOfflineEvent(String streamId, String serverId) {
        this(streamId, serverId, REASON_STREAM_OFFLINE);
    }

    public StreamOfflineEvent(String streamId, String serverId, String reason) {
        this.streamId = streamId;
        this.serverId = serverId;
        this.reason = reason == null ? REASON_STREAM_OFFLINE : reason;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getServerId() {
        return serverId;
    }

    public String getReason() {
        return reason;
    }
}
