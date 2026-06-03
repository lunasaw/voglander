package io.github.lunasaw.voglander.manager.routing;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 设备节点路由服务：维护 deviceId → nodeId 的 Redis 映射。
 */
@Service
@ConditionalOnProperty(name = "voglander.command.affinity-route.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceNodeRouteService {

    private static final String KEY_PREFIX = "dev:node:";
    private static final long   TTL_SECONDS = 60L;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void registerDevice(String deviceId, String nodeId) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + deviceId, nodeId, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void renewDevice(String deviceId) {
        stringRedisTemplate.expire(KEY_PREFIX + deviceId, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public String lookupNode(String deviceId) {
        return stringRedisTemplate.opsForValue().get(KEY_PREFIX + deviceId);
    }

    public void clear(String deviceId) {
        stringRedisTemplate.delete(KEY_PREFIX + deviceId);
    }
}
