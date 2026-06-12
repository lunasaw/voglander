package io.github.lunasaw.voglander.service.live;

import io.github.lunasaw.voglander.service.live.dto.LivePlayDTO;
import io.github.lunasaw.voglander.service.live.dto.LiveStartDTO;

/**
 * 媒体播放编排服务（直播）。
 * <p>
 * 负责选节点 → openRtpServer → 预写 INVITING 占位会话 → 发 INVITE → 等流就绪 → 拼 PlayUrls →
 * 引用计数与会话注册。单 INVITE 多路复用：同一 streamId 的多观看者共享一路拉流。
 * 回放方法（startPlayback/stopPlayback/controlPlayback）在 Sprint 2 扩展。
 * </p>
 *
 * @author luna
 */
public interface MediaPlayService {

    /**
     * 开始直播（首播 / 复用）。幂等：同通道多次调用复用同一路拉流并累加引用计数。
     *
     * @param dto 直播请求
     * @return 播放信息（streamId / callId / status / playUrls / refCount）
     */
    LivePlayDTO startLive(LiveStartDTO dto);

    /**
     * 停止直播（引用计数 -1；归零后延迟回收）。
     *
     * @param streamId 流标识
     * @return 是否成功受理
     */
    boolean stopLive(String streamId);

    /**
     * 查询直播状态（轮询兜底）。
     *
     * @param streamId 流标识
     * @return 播放信息，不存在抛 {@code LIVE_STREAM_NOT_FOUND}
     */
    LivePlayDTO getLive(String streamId);

    /**
     * 心跳续约（防误回收）。
     *
     * @param streamId 流标识
     */
    void keepAlive(String streamId);

    /**
     * 真实关流（GC 空闲回收终态）。复用节点解析能力：
     * {@code getByStreamId → nodeServerId → ZlmNode}，执行 {@code closeRtpServer} + {@code sendBye}，
     * 标会话 CLOSED，发 SSE {@code live.closed}，清 Registry 与 pending_close 标记。
     * <p>
     * 幂等：会话不存在或下游失败均尽力收尾（清 Registry/删 key），不抛异常阻断 GC。
     *
     * @param streamId 流标识
     */
    void closeStream(String streamId);

    /**
     * 真实关流（带来源 reason）。收尾动作同 {@link #closeStream(String)}，
     * 但 SSE {@code live.closed} 的 {@code reason} 取传入值，便于前端与排障区分来源
     * （{@code stream_offline} / {@code none_reader} / {@code idle_gc} / {@code node_exited}）。
     * <p>
     * 含**标准对话内 BYE**（{@code sendBye(callId)}）——平台作为 UAC 主动结束会话，
     * 符合 SIP/GB28181：established 对话由一方发 BYE 终止，而非单方面删缓存。
     *
     * @param streamId 流标识
     * @param reason   下线来源（透传到 SSE {@code reason}），null 视为 {@code idle_gc}
     */
    void closeStream(String streamId, String reason);
}
