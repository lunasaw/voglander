package io.github.lunasaw.voglander.common.event;

/**
 * ZLM 流下线事件。
 * <p>
 * 由 {@code VoglanderZlmHookServiceImpl.onStreamChanged(regist=false)} 发布（integration 层），
 * 由 {@code LiveStreamEventListener} 消费（service 层，清直播缓存 + 标 DB CLOSED + 推 SSE live.closed）。
 * 与 {@link StreamReadyEvent} 对称，置于 voglander-common 打破 service→integration 循环依赖。
 * </p>
 *
 * @author luna
 */
public class StreamOfflineEvent {

    /**
     * 流标识（ZLM stream id，即 streamId）。
     */
    private final String streamId;

    /**
     * ZLM mediaServerId，便于定位/日志，可为 null。
     */
    private final String serverId;

    public StreamOfflineEvent(String streamId, String serverId) {
        this.streamId = streamId;
        this.serverId = serverId;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getServerId() {
        return serverId;
    }
}
