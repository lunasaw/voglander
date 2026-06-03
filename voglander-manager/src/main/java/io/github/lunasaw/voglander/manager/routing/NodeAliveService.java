package io.github.lunasaw.voglander.manager.routing;

import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 节点存活服务：维护节点心跳。
 */
@Service
@ConditionalOnProperty(name = "voglander.command.affinity-route.enabled", havingValue = "true", matchIfMissing = false)
public class NodeAliveService {

    private static final String KEY_PREFIX  = "node:alive:";
    private static final long   TTL_SECONDS = 15L;

    @Value("${gateway.node-id:node-1}")
    private String              localNodeId;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void init() {
        registerNode();
    }

    @Scheduled(fixedRate = 5000)
    public void heartbeat() {
        registerNode();
    }

    private void registerNode() {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + localNodeId, "1", TTL_SECONDS, TimeUnit.SECONDS);
    }

    public boolean isAlive(String nodeId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + nodeId));
    }

    public String getLocalNodeId() {
        return localNodeId;
    }
}
