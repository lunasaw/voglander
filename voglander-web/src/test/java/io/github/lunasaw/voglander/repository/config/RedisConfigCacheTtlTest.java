package io.github.lunasaw.voglander.repository.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * Phase 1：设备缓存短 TTL 配置单元测试。
 * <p>
 * 校验 {@link RedisConfig#deviceCacheTtls()} 为 {@code device} 单对象缓存与
 * {@code device:list} 列表缓存提供有界短 TTL（收敛 cache-aside 脏读窗口，修 H4），
 * 二者隔离、列表 TTL 更短，单对象写不连坐列表。
 * </p>
 * <p>
 * 纯静态方法单测，不依赖 Spring / Redis。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class RedisConfigCacheTtlTest {

    @Test
    public void testDeviceCacheShortTtl() {
        Map<String, Duration> ttls = RedisConfig.deviceCacheTtls();

        Duration deviceTtl = ttls.get("device");
        assertEquals(Duration.ofMinutes(3), deviceTtl,
            "device 单对象缓存应为短 TTL（3min），收敛脏读窗口");

        Duration listTtl = ttls.get("device:list");
        assertEquals(Duration.ofSeconds(60), listTtl,
            "device:list 列表缓存应为更短 TTL（60s），与单对象隔离");

        assertTrue(listTtl.compareTo(deviceTtl) < 0,
            "列表缓存 TTL 必须短于单对象缓存 TTL");

        log.info("设备缓存短 TTL 配置校验通过：device={}, device:list={}", deviceTtl, listTtl);
    }
}
