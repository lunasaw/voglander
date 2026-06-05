package io.github.lunasaw.voglander.common.event;

/**
 * ZLM 流上线事件。
 * <p>
 * 由 {@code VoglanderZlmHookServiceImpl.onStreamChanged} 发布（integration 层），
 * 由 {@code LiveStreamEventListener} 消费（service 层，完成首播 future + 推 SSE live.ready）。
 * 置于 voglander-common 以打破 service→integration 的循环依赖。
 * </p>
 *
 * @author luna
 */
public class StreamReadyEvent {

    /**
     * 流标识（ZLM stream id，即 streamId）。
     */
    private final String streamId;

    public StreamReadyEvent(String streamId) {
        this.streamId = streamId;
    }

    public String getStreamId() {
        return streamId;
    }
}
