package io.github.lunasaw.voglander.intergration.wrapper.gb28181.store;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.sipgateway.gb28181.store.InviteContext;
import io.github.lunasaw.sipgateway.gb28181.store.InviteContextStore;
import lombok.extern.slf4j.Slf4j;

/**
 * 多节点 INVITE 上下文存储（Redis 实现）。
 * <p>
 * 替换默认的 {@code InMemoryInviteContextStore}，以 {@code callId} 为键存
 * {@link InviteContext}(nodeId, ctxKey) 二元组，支撑跨节点 INVITE 回包路由。
 * 仅当 {@code gateway.gb28181.store.type=redis} 时启用。
 * </p>
 *
 * <p>
 * 契约（{@code TransactionContextStore}）：
 * </p>
 * <ul>
 * <li>{@link #find(String)} 返回 null 表示不存在（业务侧回包 → 410）。</li>
 * <li>Redis 后端故障必须抛 {@link ResponseStatusException}(503)，不可静默吞掉。</li>
 * </ul>
 *
 * @author luna
 * @since 2025-05-29
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "gateway.gb28181.store.type", havingValue = "redis")
public class RedisInviteContextStore implements InviteContextStore {

    /**
     * INVITE 上下文 Redis key 前缀。
     */
    private static final String KEY_PREFIX = "sip:invite:ctx:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public RedisInviteContextStore() {
        log.info("RedisInviteContextStore active — 多节点 INVITE 上下文经 Redis 路由");
    }

    @Override
    public void save(String callId, InviteContext value, long ttlMs) {
        try {
            String json = JSON.toJSONString(value);
            stringRedisTemplate.opsForValue().set(buildKey(callId), json, Duration.ofMillis(ttlMs));
        } catch (Exception e) {
            log.error("保存 INVITE 上下文到 Redis 失败, callId={}", callId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INVITE 上下文存储不可用", e);
        }
    }

    @Override
    public InviteContext find(String callId) {
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(buildKey(callId));
        } catch (Exception e) {
            log.error("从 Redis 读取 INVITE 上下文失败, callId={}", callId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INVITE 上下文存储不可用", e);
        }
        if (json == null) {
            return null;
        }
        try {
            return JSON.parseObject(json, InviteContext.class);
        } catch (Exception e) {
            log.error("反序列化 INVITE 上下文失败, callId={}, raw={}", callId, json, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "INVITE 上下文数据损坏", e);
        }
    }

    @Override
    public void remove(String callId) {
        try {
            stringRedisTemplate.delete(buildKey(callId));
        } catch (Exception e) {
            // 删除失败仅告警：TTL 兜底会自动过期，不影响主流程
            log.warn("删除 INVITE 上下文失败, callId={}, 依赖 TTL 自动过期: {}", callId, e.getMessage());
        }
    }

    private String buildKey(String callId) {
        return KEY_PREFIX + callId;
    }
}
