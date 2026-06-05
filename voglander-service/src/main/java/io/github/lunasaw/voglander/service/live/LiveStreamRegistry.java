package io.github.lunasaw.voglander.service.live;

import java.util.concurrent.CompletableFuture;

/**
 * 直播流注册中心。
 * <p>
 * 以 {@code streamId} 为键，管理直播/回放会话的引用计数、会话快照、首播就绪 future 与 TTL 续约。
 * 引用计数与会话快照外置 Redis（集群共享），首播就绪 future 为节点本地协调（跨节点由 Pub/Sub 唤醒）。
 * </p>
 *
 * @author luna
 */
public interface LiveStreamRegistry {

    /**
     * 增加引用计数，返回新值。
     *
     * @param streamId 流标识
     * @return 自增后的引用计数
     */
    long incRef(String streamId);

    /**
     * 减少引用计数，返回新值（不低于 0）。
     *
     * @param streamId 流标识
     * @return 自减后的引用计数（>=0）
     */
    long decRef(String streamId);

    /**
     * 读取引用计数。
     *
     * @param streamId 流标识
     * @return 当前引用计数（无记录返回 0）
     */
    long getRef(String streamId);

    /**
     * 存储会话信息（默认 TTL）。
     *
     * @param streamId 流标识
     * @param info 会话信息
     */
    void putSession(String streamId, LiveSessionInfo info);

    /**
     * 读取会话信息。
     *
     * @param streamId 流标识
     * @return 会话信息，无则 null
     */
    LiveSessionInfo getSession(String streamId);

    /**
     * 删除会话（流关闭时清理 session + refcount + 本地 future）。
     *
     * @param streamId 流标识
     */
    void remove(String streamId);

    /**
     * 注册首播等待 future（流就绪前阻塞）。
     *
     * @param streamId 流标识
     * @param future 等待 future
     */
    void registerFuture(String streamId, CompletableFuture<Void> future);

    /**
     * 触发 future 完成（由 onStreamChanged 流就绪回调驱动）。
     *
     * @param streamId 流标识
     */
    void completeFuture(String streamId);

    /**
     * 续约 session/refcount 的 TTL。
     *
     * @param streamId 流标识
     * @param ttlSeconds TTL 秒数
     */
    void keepAlive(String streamId, long ttlSeconds);
}
