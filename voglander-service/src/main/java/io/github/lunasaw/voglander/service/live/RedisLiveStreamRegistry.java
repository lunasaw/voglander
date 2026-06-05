package io.github.lunasaw.voglander.service.live;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于 Redis 的直播流注册中心实现。
 * <p>
 * 引用计数用 {@link StringRedisTemplate} 的原子 INCR/DECR（DECR 经 Lua 脚本保证不低于 0），
 * 会话快照以 FastJSON2 字符串存于 String 结构；首播就绪 future 为节点本地 {@link ConcurrentHashMap}，
 * 跨节点流就绪由 SSE/Pub-Sub 通道唤醒（本类只负责本地 future）。
 * </p>
 * <p>
 * 使用主 Redis-A（{@code stringRedisTemplate}），不混入 GB28181 invite 专用的 Redis-B。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class RedisLiveStreamRegistry implements LiveStreamRegistry {

    private static final String                  PREFIX_SESSION         = "live:session:";
    private static final String                  PREFIX_REF             = "live:refcount:";
    private static final long                    DEFAULT_TTL_SEC        = 3600;

    /**
     * DECR 且不低于 0 的 Lua 脚本：先 DECR，若结果 &lt;0 则重置为 0。
     */
    private static final RedisScript<Long>       DECR_NOT_NEGATIVE      = RedisScript.of(
        "local v=redis.call('DECR',KEYS[1]) if v<0 then redis.call('SET',KEYS[1],'0') return 0 end return v",
        Long.class);

    @Autowired
    private StringRedisTemplate                  stringRedisTemplate;

    private final ConcurrentHashMap<String, CompletableFuture<Void>> localFutures = new ConcurrentHashMap<>();

    @Override
    public long incRef(String streamId) {
        Long v = stringRedisTemplate.opsForValue().increment(PREFIX_REF + streamId);
        return v == null ? 0 : v;
    }

    @Override
    public long decRef(String streamId) {
        Long v = stringRedisTemplate.execute(DECR_NOT_NEGATIVE, List.of(PREFIX_REF + streamId));
        return v == null ? 0 : v;
    }

    @Override
    public long getRef(String streamId) {
        String v = stringRedisTemplate.opsForValue().get(PREFIX_REF + streamId);
        if (v == null) {
            return 0;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void putSession(String streamId, LiveSessionInfo info) {
        String json = JSON.toJSONString(info);
        stringRedisTemplate.opsForValue().set(PREFIX_SESSION + streamId, json, DEFAULT_TTL_SEC, TimeUnit.SECONDS);
    }

    @Override
    public LiveSessionInfo getSession(String streamId) {
        String json = stringRedisTemplate.opsForValue().get(PREFIX_SESSION + streamId);
        return json == null ? null : JSON.parseObject(json, LiveSessionInfo.class);
    }

    @Override
    public void remove(String streamId) {
        stringRedisTemplate.delete(List.of(PREFIX_SESSION + streamId, PREFIX_REF + streamId));
        localFutures.remove(streamId);
    }

    @Override
    public void registerFuture(String streamId, CompletableFuture<Void> future) {
        localFutures.put(streamId, future);
    }

    @Override
    public void completeFuture(String streamId) {
        CompletableFuture<Void> f = localFutures.remove(streamId);
        if (f != null) {
            f.complete(null);
            log.debug("唤醒首播等待 future, streamId={}", streamId);
        }
    }

    @Override
    public void keepAlive(String streamId, long ttlSeconds) {
        stringRedisTemplate.expire(PREFIX_SESSION + streamId, ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.expire(PREFIX_REF + streamId, ttlSeconds, TimeUnit.SECONDS);
    }
}
