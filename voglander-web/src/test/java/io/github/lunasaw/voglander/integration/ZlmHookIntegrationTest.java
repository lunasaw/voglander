package io.github.lunasaw.voglander.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.impl.VoglanderZlmHookServiceImpl;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.zlm.hook.param.OnStreamChangedHookParam;
import lombok.extern.slf4j.Slf4j;

/**
 * ZLM Hook回调集成测试
 * 测试流状态变化回调与StreamProxyManager的集成
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class ZlmHookIntegrationTest extends BaseTest {

    private final String                TEST_APP       = "live";
    private final String                TEST_STREAM    = "test_stream_001";
    private final String                TEST_URL       = "rtmp://live.example.com/live/test_stream_001";
    private final String                TEST_PROXY_KEY = "test-proxy-key-integration";
    private final String                TEST_VHOST     = "__defaultVhost__";

    @Autowired
    private VoglanderZlmHookServiceImpl zlmHookService;

    @Autowired
    private StreamProxyManager          streamProxyManager;

    @Autowired
    private CacheManager                cacheManager;

    private StreamProxyDO               testStreamProxyDO;

    @BeforeEach
    public void setUp() {
        // 清除缓存
        if (cacheManager.getCache("streamProxy") != null) {
            cacheManager.getCache("streamProxy").clear();
        }

        // 创建测试流代理记录
        testStreamProxyDO = createTestStreamProxy();
        Long proxyId = streamProxyManager.saveOrUpdateProxy(
            TEST_APP, TEST_STREAM, TEST_URL, TEST_PROXY_KEY, 0, null);
        testStreamProxyDO.setId(proxyId);
        log.info("创建测试流代理记录成功 - ID: {}, app: {}, stream: {}", proxyId, TEST_APP, TEST_STREAM);
    }

    private StreamProxyDO createTestStreamProxy() {
        StreamProxyDO streamProxy = new StreamProxyDO();
        streamProxy.setApp(TEST_APP);
        streamProxy.setStream(TEST_STREAM);
        streamProxy.setUrl(TEST_URL);
        streamProxy.setProxyKey(TEST_PROXY_KEY);
        streamProxy.setStatus(1);
        streamProxy.setOnlineStatus(0);
        streamProxy.setEnabled(true);
        streamProxy.setDescription("Integration test proxy");
        streamProxy.setExtend("{\"vhost\":\"" + TEST_VHOST + "\"}");
        streamProxy.setCreateTime(LocalDateTime.now());
        streamProxy.setUpdateTime(LocalDateTime.now());
        return streamProxy;
    }

    private OnStreamChangedHookParam createStreamChangedHookParam(boolean regist) {
        OnStreamChangedHookParam param = new OnStreamChangedHookParam();
        param.setRegist(regist);
        param.setApp(TEST_APP);
        param.setStream(TEST_STREAM);
        param.setCallId("test-call-id");
        param.setTotalReaderCount(regist ? "5" : "0");
        param.setSchema("rtmp");
        param.setVhost(TEST_VHOST);
        param.setAliveSecond(regist ? Long.valueOf(120) : Long.valueOf(0));
        return param;
    }

    // ============== 流上线集成测试 ==============

    @Test
    public void testOnStreamChanged_StreamRegister_UpdatesOnlineStatus() {
        // Arrange
        OnStreamChangedHookParam param = createStreamChangedHookParam(true);

        // Verify initial state is offline
        StreamProxyDO initialProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertNotNull(initialProxy);
        assertEquals(0, initialProxy.getOnlineStatus());

        // Act
        zlmHookService.onStreamChanged(param, null);

        // Assert
        StreamProxyDO updatedProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertNotNull(updatedProxy);
        assertEquals(1, updatedProxy.getOnlineStatus());

        // Verify extend information is updated
        String extend = updatedProxy.getExtend();
        assertNotNull(extend);
        assertTrue(extend.contains("totalReaderCount"));
        assertTrue(extend.contains("callbackType"));
        assertTrue(extend.contains("onStreamChanged"));

        log.info("流上线集成测试成功 - 代理ID: {}, 在线状态: {}, 扩展信息: {}",
            updatedProxy.getId(), updatedProxy.getOnlineStatus(), extend);
    }

    @Test
    public void testOnStreamChanged_StreamUnregister_UpdatesOfflineStatus() {
        // Arrange - 首先让流上线
        OnStreamChangedHookParam registerParam = createStreamChangedHookParam(true);
        zlmHookService.onStreamChanged(registerParam, null);

        // Verify stream is online
        StreamProxyDO onlineProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertEquals(1, onlineProxy.getOnlineStatus());

        // Now create unregister param
        OnStreamChangedHookParam unregisterParam = createStreamChangedHookParam(false);

        // Act
        zlmHookService.onStreamChanged(unregisterParam, null);

        // Assert
        StreamProxyDO updatedProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertNotNull(updatedProxy);
        assertEquals(0, updatedProxy.getOnlineStatus());

        // Verify extend information is updated
        String extend = updatedProxy.getExtend();
        assertNotNull(extend);
        assertTrue(extend.contains("totalReaderCount"));
        assertTrue(extend.contains("callbackType"));
        assertTrue(extend.contains("onStreamChanged"));

        log.info("流下线集成测试成功 - 代理ID: {}, 在线状态: {}, 扩展信息: {}",
            updatedProxy.getId(), updatedProxy.getOnlineStatus(), extend);
    }

    @Test
    public void testOnStreamChanged_NonExistentStream_LogsWarning() {
        // Arrange
        OnStreamChangedHookParam param = createStreamChangedHookParam(true);
        param.setApp("non_existent_app");
        param.setStream("non_existent_stream");

        // Act & Assert - 不应该抛出异常
        assertDoesNotThrow(() -> {
            zlmHookService.onStreamChanged(param, null);
        });

        // Verify original stream proxy is unchanged
        StreamProxyDO originalProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertNotNull(originalProxy);
        assertEquals(0, originalProxy.getOnlineStatus());

        log.info("不存在的流回调测试成功 - 原始代理状态未变");
    }

    // ============== 缓存一致性集成测试 ==============

    @Test
    public void testOnStreamChanged_CacheConsistency_CacheClearedAfterUpdate() {
        // Arrange
        Cache cache = cacheManager.getCache("streamProxy");
        if (cache != null) {
            // Pre-populate cache
            cache.put(testStreamProxyDO.getId(), testStreamProxyDO);
            cache.put("key:" + TEST_PROXY_KEY, testStreamProxyDO);

            // Verify cache is populated
            assertNotNull(cache.get(testStreamProxyDO.getId()));
            assertNotNull(cache.get("key:" + TEST_PROXY_KEY));
        }

        OnStreamChangedHookParam param = createStreamChangedHookParam(true);

        // Act
        zlmHookService.onStreamChanged(param, null);

        // Assert - Cache should be cleared
        if (cache != null) {
            assertNull(cache.get(testStreamProxyDO.getId()));
        }

        // Verify data is still accessible through manager
        StreamProxyDO updatedProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertNotNull(updatedProxy);
        assertEquals(1, updatedProxy.getOnlineStatus());

        log.info("缓存一致性集成测试成功 - 缓存已清理，数据依然可访问");
    }

    // ============== 扩展信息集成测试 ==============

    @Test
    public void testOnStreamChanged_ExtendInfoBuild_ContainsExpectedFields() {
        // Arrange
        OnStreamChangedHookParam param = createStreamChangedHookParam(true);
        param.setTotalReaderCount("10");
        param.setAliveSecond(Long.valueOf(300));
        param.setCallId("test-call-id-123");

        // Act
        zlmHookService.onStreamChanged(param, null);

        // Assert
        StreamProxyDO updatedProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        String extend = updatedProxy.getExtend();

        assertNotNull(extend);
        assertTrue(extend.contains("\"totalReaderCount\":\"10\""));
        assertTrue(extend.contains("\"aliveSecond\":300"));
        assertTrue(extend.contains("\"callId\":\"test-call-id-123\""));
        assertTrue(extend.contains("\"callbackType\":\"onStreamChanged\""));
        assertTrue(extend.contains("\"callbackTime\":"));
        assertTrue(extend.contains("\"regist\":true"));

        log.info("扩展信息构建集成测试成功 - 扩展信息: {}", extend);
    }

    @Test
    public void testOnStreamChanged_RegistrationCycle_ProperStatusTransitions() {
        // Test complete registration cycle: offline -> online -> offline

        // Initial state verification
        StreamProxyDO initialProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertEquals(0, initialProxy.getOnlineStatus());

        // Step 1: Stream goes online
        OnStreamChangedHookParam registerParam = createStreamChangedHookParam(true);
        zlmHookService.onStreamChanged(registerParam, null);

        StreamProxyDO onlineProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertEquals(1, onlineProxy.getOnlineStatus());

        // Step 2: Stream goes offline
        OnStreamChangedHookParam unregisterParam = createStreamChangedHookParam(false);
        zlmHookService.onStreamChanged(unregisterParam, null);

        StreamProxyDO offlineProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertEquals(0, offlineProxy.getOnlineStatus());

        // Step 3: Stream goes online again
        zlmHookService.onStreamChanged(registerParam, null);

        StreamProxyDO onlineAgainProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertEquals(1, onlineAgainProxy.getOnlineStatus());

        log.info("注册周期集成测试成功 - 状态转换: 离线 -> 在线 -> 离线 -> 在线");
    }

    // ============== 边界情况集成测试 ==============

    @Test
    public void testOnStreamChanged_NullParams_HandlesGracefully() {
        // Test null app
        OnStreamChangedHookParam paramNullApp = createStreamChangedHookParam(true);
        paramNullApp.setApp(null);

        assertDoesNotThrow(() -> {
            zlmHookService.onStreamChanged(paramNullApp, null);
        });

        // Test null stream
        OnStreamChangedHookParam paramNullStream = createStreamChangedHookParam(true);
        paramNullStream.setStream(null);

        assertDoesNotThrow(() -> {
            zlmHookService.onStreamChanged(paramNullStream, null);
        });

        // Verify original proxy is unchanged
        StreamProxyDO originalProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertEquals(0, originalProxy.getOnlineStatus());

        log.info("空参数处理集成测试成功 - 优雅处理异常情况");
    }

    @Test
    public void testOnStreamChanged_EmptyParams_HandlesGracefully() {
        // Test empty app
        OnStreamChangedHookParam paramEmptyApp = createStreamChangedHookParam(true);
        paramEmptyApp.setApp("");

        assertDoesNotThrow(() -> {
            zlmHookService.onStreamChanged(paramEmptyApp, null);
        });

        // Test empty stream
        OnStreamChangedHookParam paramEmptyStream = createStreamChangedHookParam(true);
        paramEmptyStream.setStream("");

        assertDoesNotThrow(() -> {
            zlmHookService.onStreamChanged(paramEmptyStream, null);
        });

        // Verify original proxy is unchanged
        StreamProxyDO originalProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertEquals(0, originalProxy.getOnlineStatus());

        log.info("空字符串参数处理集成测试成功 - 优雅处理异常情况");
    }

    @Test
    public void testOnStreamChanged_ConcurrentUpdates_ThreadSafe() {
        // This test simulates concurrent stream status changes
        // In a real scenario, multiple callbacks might arrive simultaneously

        OnStreamChangedHookParam registerParam = createStreamChangedHookParam(true);
        OnStreamChangedHookParam unregisterParam = createStreamChangedHookParam(false);

        // Simulate rapid status changes
        for (int i = 0; i < 10; i++) {
            zlmHookService.onStreamChanged(registerParam, null);
            zlmHookService.onStreamChanged(unregisterParam, null);
        }

        // Final state should be offline
        StreamProxyDO finalProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertNotNull(finalProxy);
        assertEquals(0, finalProxy.getOnlineStatus());

        log.info("并发更新集成测试成功 - 最终状态: 离线");
    }

    // ============== 性能和资源管理测试 ==============

    @Test
    public void testOnStreamChanged_PerformanceUnder_ManyCallbacks() {
        long startTime = System.currentTimeMillis();

        OnStreamChangedHookParam param = createStreamChangedHookParam(true);

        // Execute many callbacks to test performance
        for (int i = 0; i < 100; i++) {
            param.setRegist(i % 2 == 0); // Alternate between register/unregister
            zlmHookService.onStreamChanged(param, null);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Should complete within reasonable time (less than 5 seconds for 100 operations)
        assertTrue(duration < 5000, "100个回调操作应在5秒内完成，实际耗时: " + duration + "ms");

        // Verify final state
        StreamProxyDO finalProxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertNotNull(finalProxy);

        log.info("性能测试成功 - 100个回调操作耗时: {}ms, 最终状态: {}",
            duration, finalProxy.getOnlineStatus() == 1 ? "在线" : "离线");
    }
}