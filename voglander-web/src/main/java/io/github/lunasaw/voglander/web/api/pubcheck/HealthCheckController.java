package io.github.lunasaw.voglander.web.api.pubcheck;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查控制器（Phase 8-G 完整化）
 * 依赖级健康检查：Redis-A / Redis-B / DB / ZLM 熔断器
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1)
public class HealthCheckController {

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    @Qualifier("inviteStringRedisTemplate")
    private StringRedisTemplate inviteStringRedisTemplate;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * 简单健康检查（向后兼容）
     */
    @GetMapping("/check")
    public AjaxResult check() {
        return AjaxResult.success("ok");
    }

    /**
     * 依赖级健康检查
     * 返回结构：{status: UP/DEGRADED/DOWN, components: {db, redis-A, redis-B, zlm}}
     */
    @GetMapping("/health")
    public AjaxResult<Map<String, Object>> health() {
        Map<String, Object> components = new LinkedHashMap<>();
        boolean anyDown = false;

        // DB 健康
        components.put("db", checkDb());
        anyDown |= "DOWN".equals(((Map<?, ?>) components.get("db")).get("status"));

        // Redis-A (业务 Redis)
        components.put("redis-A", checkRedis(stringRedisTemplate));
        anyDown |= "DOWN".equals(((Map<?, ?>) components.get("redis-A")).get("status"));

        // Redis-B (INVITE context，仅当独立配置存在时上报)
        if (inviteStringRedisTemplate != null && inviteStringRedisTemplate != stringRedisTemplate) {
            components.put("redis-B", checkRedis(inviteStringRedisTemplate));
            anyDown |= "DOWN".equals(((Map<?, ?>) components.get("redis-B")).get("status"));
        }

        // ZLM 熔断器状态
        if (circuitBreakerRegistry != null) {
            components.put("zlm", checkZlmCircuit());
            anyDown |= "DOWN".equals(((Map<?, ?>) components.get("zlm")).get("status"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", anyDown ? "DEGRADED" : "UP");
        result.put("components", components);
        return AjaxResult.success(result);
    }

    private Map<String, Object> checkDb() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (dataSource == null) {
            m.put("status", "UNKNOWN");
            return m;
        }
        try {
            boolean valid = dataSource.getConnection().isValid(1);
            m.put("status", valid ? "UP" : "DOWN");
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> checkRedis(StringRedisTemplate template) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (template == null) {
            m.put("status", "UNKNOWN");
            return m;
        }
        try {
            long start = System.nanoTime();
            template.opsForValue().get("__health_ping__");
            long ms = (System.nanoTime() - start) / 1_000_000;
            m.put("status", "UP");
            m.put("ping_ms", ms);
        } catch (Exception e) {
            m.put("status", "DOWN");
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Map<String, Object> checkZlmCircuit() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("zlm");
            CircuitBreaker.State state = cb.getState();
            String status;
            switch (state) {
                case CLOSED:
                    status = "UP";
                    break;
                case HALF_OPEN:
                    status = "DEGRADED";
                    break;
                case OPEN:
                case FORCED_OPEN:
                    status = "DOWN";
                    break;
                default:
                    status = "UNKNOWN";
            }
            m.put("status", status);
            m.put("circuit_state", state.name());
        } catch (Exception e) {
            m.put("status", "UNKNOWN");
            m.put("error", e.getMessage());
        }
        return m;
    }
}
