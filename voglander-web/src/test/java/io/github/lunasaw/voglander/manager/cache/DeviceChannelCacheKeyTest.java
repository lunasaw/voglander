package io.github.lunasaw.voglander.manager.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Stage 0 验收：DeviceChannelCacheKey key 格式
 */
class DeviceChannelCacheKeyTest {

    @Test
    void byId_format() {
        assertEquals("id:1", DeviceChannelCacheKey.byId(1L));
    }

    @Test
    void byBizKey_format() {
        assertEquals("biz:d:c", DeviceChannelCacheKey.byBizKey("d", "c"));
    }

    @Test
    void byDevice_format() {
        assertEquals("device:d", DeviceChannelCacheKey.byDevice("d"));
    }
}
