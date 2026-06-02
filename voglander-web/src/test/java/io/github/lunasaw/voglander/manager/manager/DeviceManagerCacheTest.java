package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.cache.DeviceCacheKey;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 1：设备缓存正确性集成测试（P1 去全清 + P2 key 统一）
 * <p>
 * 同时钉死两个缺陷，必须同时通过才算修复：
 * </p>
 * <ul>
 * <li><b>P2（key 统一）</b>：{@code @Cacheable} 写入的 key 与 {@code clearCache} evict 的 key 必须一致，
 * 否则精确 evict 永不���中——更新后读到陈旧值。断言：更新设备后再次读取返回最新值。</li>
 * <li><b>P1（去全清）</b>：单对象写不得 {@code cache.clear()} 连坐整个缓存区。
 * 断言：更新设备 X 不得清掉无关设备 Y 的缓存。</li>
 * </ul>
 * <p>
 * 旧代码下：X 因 cache.clear() 仍为最新（P2 被掩盖），但 Y 被连坐清掉 → P1 断言失败；
 * 仅去 clear 不统一 key：X evict 不命中 → 读到陈旧 → P2 断言失败。两条断言共同锁死 P1+P2。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class DeviceManagerCacheTest extends BaseTest {

    private static final String TEST_DEVICE_X = "CACHE_TEST_DEVICE_X";
    private static final String TEST_DEVICE_Y = "CACHE_TEST_DEVICE_Y";
    private static final String TEST_IP        = "192.168.99.99";

    @Autowired
    private DeviceManager  deviceManager;

    @Autowired
    private DeviceService  deviceService;

    @Autowired
    private CacheManager   cacheManager;

    @BeforeEach
    public void setUp() {
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
        // 清理缓存区，避免跨用例污染
        Cache cache = cacheManager.getCache(DeviceCacheKey.CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    private void cleanup() {
        deviceService.lambdaUpdate()
            .in(DeviceDO::getDeviceId, TEST_DEVICE_X, TEST_DEVICE_Y)
            .remove();
        Cache cache = cacheManager.getCache(DeviceCacheKey.CACHE_NAME);
        if (cache != null) {
            cache.evict(DeviceCacheKey.byDeviceId(TEST_DEVICE_X));
            cache.evict(DeviceCacheKey.byDeviceId(TEST_DEVICE_Y));
        }
    }

    private Long createDevice(String deviceId) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(deviceId);
        dto.setIp(TEST_IP);
        dto.setPort(5060);
        dto.setType(1);
        dto.setServerIp("127.0.0.1");
        dto.setName("origin-" + deviceId);
        return deviceManager.add(dto);
    }

    @Test
    public void testPreciseEvictHitsAlignedKeyAndKeepsOthers() {
        // Given：两台设备入库并各自填充缓存
        Long idX = createDevice(TEST_DEVICE_X);
        createDevice(TEST_DEVICE_Y);

        DeviceDTO cachedX = deviceManager.getDtoByDeviceId(TEST_DEVICE_X);
        DeviceDTO cachedY = deviceManager.getDtoByDeviceId(TEST_DEVICE_Y);
        assertNotNull(cachedX);
        assertNotNull(cachedY);

        Cache cache = cacheManager.getCache(DeviceCacheKey.CACHE_NAME);
        assertNotNull(cache, "device 缓存区必须存在");
        assertNotNull(cache.get(DeviceCacheKey.byDeviceId(TEST_DEVICE_X)), "X 应已进缓存（key 对齐前提）");
        assertNotNull(cache.get(DeviceCacheKey.byDeviceId(TEST_DEVICE_Y)), "Y 应已进缓存");

        // When：仅更新 X
        DeviceDTO updateDTO = new DeviceDTO();
        updateDTO.setName("updated-name-X");
        deviceManager.updateById(idX, updateDTO);

        // Then-P2：X 的缓存被精确 evict 命中（说明 @Cacheable 与 evict 的 key 一致）
        assertNull(cache.get(DeviceCacheKey.byDeviceId(TEST_DEVICE_X)),
            "P2：更新 X 后其缓存必须被精确 evict（key 必须与 @Cacheable 一致）");

        // Then-P1：Y 的缓存未被连坐清除（说明不再 cache.clear()）
        assertNotNull(cache.get(DeviceCacheKey.byDeviceId(TEST_DEVICE_Y)),
            "P1：更新 X 不得连坐清除无关设备 Y 的缓存（禁止 cache.clear()）");

        // 再次读取 X，必须是最新值（陈旧读验证）
        DeviceDTO freshX = deviceManager.getDtoByDeviceId(TEST_DEVICE_X);
        assertNotNull(freshX);
        assertEquals("updated-name-X", freshX.getName(),
            "P2：更新后再次读取必须返回最新值，不得陈旧");

        log.info("P1+P2 缓存正确性校验通过：精确 evict 命中且无连坐清除");
    }
}
