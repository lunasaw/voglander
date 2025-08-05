package io.github.lunasaw.voglander.zlm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.impl.VoglanderZlmHookServiceImpl;
import io.github.lunasaw.zlm.hook.param.OnProxyAddedHookParam;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * StreamProxy端到端集成测试
 * 
 * 测试覆盖完整流程：
 * 1. zlm-starter接口调用 (Mock)
 * 2. 数据库存储
 * 3. Hook回调处理
 * 4. 状态更新入库
 * 5. 缓存验证
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@DisplayName("StreamProxy端到端集成测试")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class StreamProxyEndToEndIntegrationTest extends BaseStreamProxyIntegrationTest {

    @Autowired
    private VoglanderZlmHookServiceImpl hookService;

    @Test
    @DisplayName("01_完整创建流程测试")
    public void test01_CompleteCreateFlow() throws Exception {
        log.info("=== 开始完整创建流程测试 ===");

        // 第一步：通过Manager创建拉流代理（模拟Controller层调用）
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp(TEST_APP);
        dto.setStream(TEST_STREAM);
        dto.setUrl(TEST_URL);
        dto.setDescription(TEST_DESCRIPTION);
        dto.setEnabled(true);

        Long proxyId = streamProxyManager.createStreamProxy(dto);
        assertNotNull(proxyId);
        log.info("步骤1完成 - 创建拉流代理成功，ID: {}", proxyId);

        // 第二步：验证数据库记录
        StreamProxyDO savedProxy = streamProxyManager.getById(proxyId);
        assertNotNull(savedProxy);
        assertEquals(TEST_APP, savedProxy.getApp());
        assertEquals(TEST_STREAM, savedProxy.getStream());
        assertEquals(TEST_URL, savedProxy.getUrl());
        assertEquals(0, savedProxy.getOnlineStatus()); // 初始离线状态
        assertNull(savedProxy.getProxyKey()); // 初始时没有proxy key
        log.info("步骤2完成 - 数据库记录验证通过");

        // 第三步：模拟ZLM Hook回调
        OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
        hookParam.setApp(TEST_APP);
        hookParam.setStream(TEST_STREAM);
        hookParam.setUrl(TEST_URL);
        hookParam.setKey(TEST_PROXY_KEY);
        hookParam.setVhost("__defaultVhost__");
        hookParam.setRetryCount(3);
        hookParam.setRtpType(0);
        hookParam.setTimeoutSec(10);
        hookParam.setEnableHls(true);
        hookParam.setEnableMp4(true);
        hookParam.setEnableRtsp(true);
        hookParam.setEnableRtmp(true);
        hookParam.setEnableTs(true);

        // 直接调用Hook处理方法
        hookService.onProxyAdded(hookParam, null);
        log.info("步骤3完成 - Hook回调处理完成");

        // 等待异步处理完成
        waitForAsyncOperation(200);

        // 第四步：验证Hook回调后的状态更新
        StreamProxyDO updatedProxy = streamProxyManager.getById(proxyId);
        assertNotNull(updatedProxy);
        assertEquals(TEST_PROXY_KEY, updatedProxy.getProxyKey());
        assertEquals(1, updatedProxy.getOnlineStatus()); // Hook回调后应该是在线状态
        assertNotNull(updatedProxy.getExtend()); // 应该有扩展信息
        log.info("步骤4完成 - Hook回调后状态更新验证通过");

        // 第五步：通过不同查询方式验证数据一致性
        StreamProxyDO proxyByKey = streamProxyManager.getByProxyKey(TEST_PROXY_KEY);
        assertNotNull(proxyByKey);
        assertEquals(proxyId, proxyByKey.getId());

        StreamProxyDO proxyByAppStream = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM);
        assertNotNull(proxyByAppStream);
        assertEquals(proxyId, proxyByAppStream.getId());
        log.info("步骤5完成 - 多种查询方式数据一致性验证通过");

        log.info("=== 完整创建流程测试通过 ===");
    }

    @Test
    @DisplayName("02_并发Hook回调处理测试")
    public void test02_ConcurrentHookCallbackTest() throws Exception {
        log.info("=== 开始并发Hook回调处理测试 ===");

        // 创建多个拉流代理
        int concurrentCount = 5;
        CompletableFuture<Void>[] futures = new CompletableFuture[concurrentCount];

        for (int i = 0; i < concurrentCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    String stream = TEST_STREAM + "_concurrent_" + index;
                    String proxyKey = TEST_PROXY_KEY + "_" + index;

                    // 创建代理
                    StreamProxyDTO dto = new StreamProxyDTO();
                    dto.setApp(TEST_APP);
                    dto.setStream(stream);
                    dto.setUrl(TEST_URL + "_" + index);
                    dto.setDescription(TEST_DESCRIPTION + "_" + index);
                    dto.setEnabled(true);

                    Long proxyId = streamProxyManager.createStreamProxy(dto);
                    log.info("并发测试 - 创建代理 {} 成功，ID: {}", index, proxyId);

                    // 模拟Hook回调
                    OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
                    hookParam.setApp(TEST_APP);
                    hookParam.setStream(stream);
                    hookParam.setUrl(TEST_URL + "_" + index);
                    hookParam.setKey(proxyKey);
                    hookParam.setVhost("__defaultVhost__");
                    hookParam.setEnableHls(true);
                    hookParam.setEnableMp4(true);

                    hookService.onProxyAdded(hookParam, null);
                    log.info("并发测试 - Hook回调 {} 处理完成", index);

                } catch (Exception e) {
                    log.error("并发测试第 {} 个任务失败: {}", index, e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        // 验证所有代理都创建成功并处理了Hook回调
        waitForAsyncOperation(500);

        for (int i = 0; i < concurrentCount; i++) {
            String stream = TEST_STREAM + "_concurrent_" + i;
            String proxyKey = TEST_PROXY_KEY + "_" + i;

            StreamProxyDO proxy = streamProxyManager.getByAppAndStream(TEST_APP, stream);
            assertNotNull(proxy, "并发测试中代理 " + i + " 应该存在");
            assertEquals(proxyKey, proxy.getProxyKey(), "并发测试中代理 " + i + " 的proxyKey应该正确");
            assertEquals(1, proxy.getOnlineStatus(), "并发测试中代理 " + i + " 应该是在线状态");
        }

        log.info("=== 并发Hook回调处理测试通过 ===");
    }

    @Test
    @DisplayName("03_Hook回调异常处理测试")
    public void test03_HookCallbackExceptionHandling() throws Exception {
        log.info("=== 开始Hook回调异常处理测试 ===");

        // 测试场景1：Hook回调参数不完整
        OnProxyAddedHookParam invalidParam = new OnProxyAddedHookParam();
        invalidParam.setApp(null); // 缺少app参数
        invalidParam.setStream(TEST_STREAM);
        invalidParam.setUrl(TEST_URL);
        invalidParam.setKey(TEST_PROXY_KEY);

        // Hook处理不应该抛出异常，而是记录错误日志
        assertDoesNotThrow(() -> {
            hookService.onProxyAdded(invalidParam, null);
        });
        log.info("异常处理测试 - 不完整参数处理通过");

        // 测试场景2：重复的Hook回调
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp(TEST_APP);
        dto.setStream(TEST_STREAM + "_duplicate");
        dto.setUrl(TEST_URL);
        dto.setDescription(TEST_DESCRIPTION);
        dto.setEnabled(true);

        Long proxyId = streamProxyManager.createStreamProxy(dto);

        OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
        hookParam.setApp(TEST_APP);
        hookParam.setStream(TEST_STREAM + "_duplicate");
        hookParam.setUrl(TEST_URL);
        hookParam.setKey(TEST_PROXY_KEY + "_duplicate");
        hookParam.setVhost("__defaultVhost__");

        // 第一次Hook回调
        hookService.onProxyAdded(hookParam, null);
        waitForAsyncOperation(100);

        // 第二次相同的Hook回调（重复调用）
        assertDoesNotThrow(() -> {
            hookService.onProxyAdded(hookParam, null);
        });

        waitForAsyncOperation(100);

        // 验证代理状态正确，没有因为重复回调而出现问题
        StreamProxyDO proxy = streamProxyManager.getById(proxyId);
        assertNotNull(proxy);
        assertEquals(TEST_PROXY_KEY + "_duplicate", proxy.getProxyKey());
        assertEquals(1, proxy.getOnlineStatus());

        log.info("异常处理测试 - 重复Hook回调处理通过");

        log.info("=== Hook回调异常处理测试通过 ===");
    }

    @Test
    @DisplayName("04_缓存一致性验证测试")
    public void test04_CacheConsistencyTest() throws Exception {
        log.info("=== 开始缓存一致性验证测试 ===");

        // 创建拉流代理
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp(TEST_APP);
        dto.setStream(TEST_STREAM + "_cache");
        dto.setUrl(TEST_URL);
        dto.setDescription(TEST_DESCRIPTION);
        dto.setEnabled(true);

        Long proxyId = streamProxyManager.createStreamProxy(dto);

        // 第一次查询，确保缓存已生成
        StreamProxyDO proxy1 = streamProxyManager.getById(proxyId);
        assertNotNull(proxy1);

        // Hook回调更新状态
        OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
        hookParam.setApp(TEST_APP);
        hookParam.setStream(TEST_STREAM + "_cache");
        hookParam.setUrl(TEST_URL);
        hookParam.setKey(TEST_PROXY_KEY + "_cache");
        hookParam.setVhost("__defaultVhost__");

        hookService.onProxyAdded(hookParam, null);
        waitForAsyncOperation(200);

        // 通过不同方式查询，验证缓存都已更新
        StreamProxyDO proxyById = streamProxyManager.getById(proxyId);
        StreamProxyDO proxyByKey = streamProxyManager.getByProxyKey(TEST_PROXY_KEY + "_cache");
        StreamProxyDO proxyByAppStream = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM + "_cache");

        // 验证所有查询结果一致
        assertEquals(TEST_PROXY_KEY + "_cache", proxyById.getProxyKey());
        assertEquals(TEST_PROXY_KEY + "_cache", proxyByKey.getProxyKey());
        assertEquals(TEST_PROXY_KEY + "_cache", proxyByAppStream.getProxyKey());

        assertEquals(1, proxyById.getOnlineStatus());
        assertEquals(1, proxyByKey.getOnlineStatus());
        assertEquals(1, proxyByAppStream.getOnlineStatus());

        log.info("=== 缓存一致性验证测试通过 ===");
    }
}