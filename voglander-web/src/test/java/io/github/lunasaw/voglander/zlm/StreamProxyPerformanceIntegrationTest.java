package io.github.lunasaw.voglander.zlm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.zlm.util.StreamProxyTestDataUtil;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.impl.VoglanderZlmHookServiceImpl;
import io.github.lunasaw.zlm.hook.param.OnProxyAddedHookParam;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * StreamProxy性能和并发集成测试
 * 
 * 测试覆盖：
 * 1. 大批量数据处理性能
 * 2. 并发Hook回调处理
 * 3. 高并发API调用
 * 4. 缓存性能测试
 * 5. 数据库性能测试
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@DisplayName("StreamProxy性能和并发集成测试")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class StreamProxyPerformanceIntegrationTest extends BaseStreamProxyIntegrationTest {

    @Autowired
    private StreamProxyTestDataUtil     testDataUtil;

    @Autowired
    private VoglanderZlmHookServiceImpl hookService;

    private static final String         TEST_PREFIX = "test_";

    @Test
    @DisplayName("01_批量创建代理性能测试")
    public void test01_BatchCreatePerformance() throws Exception {
        log.info("=== 开始批量创建代理性能测试 ===");

        int batchSize = 50;
        long startTime = System.currentTimeMillis();

        // 批量创建代理
        List<Long> proxyIds = testDataUtil.createBatchTestProxies(batchSize);

        long createTime = System.currentTimeMillis() - startTime;

        // 验证创建结果
        assertEquals(batchSize, proxyIds.size());
        log.info("批量创建性能 - 数量: {}, 耗时: {}ms, 平均: {}ms/个",
            batchSize, createTime, createTime / batchSize);

        // 性能基准：每个代理创建时间应小于100ms
        assertTrue(createTime / batchSize < 100, "代理创建性能不符合预期，平均耗时应小于100ms");

        // 验证数据库记录
        for (Long proxyId : proxyIds) {
            StreamProxyDO proxy = streamProxyManager.getById(proxyId);
            assertNotNull(proxy);
            assertTrue(testDataUtil.verifyProxyDataIntegrity(proxy));
        }

        log.info("=== 批量创建代理性能测试通过 ===");
    }

    @Test
    @DisplayName("02_高并发Hook回调处理测试")
    public void test02_ConcurrentHookProcessing() throws Exception {
        log.info("=== 开始高并发Hook回调处理测试 ===");

        int concurrentCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // 预先创建代理
        List<Long> proxyIds = testDataUtil.createBatchTestProxies(concurrentCount);

        long startTime = System.currentTimeMillis();

        // 并发执行Hook回调
        CompletableFuture<Void>[] futures = IntStream.range(0, concurrentCount)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    String stream = TEST_PREFIX + "stream_batch_" + i;
                    String proxyKey = TEST_PROXY_KEY + "_concurrent_" + i;

                    OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
                    hookParam.setApp(TEST_APP);
                    hookParam.setStream(stream);
                    hookParam.setUrl(TEST_URL);
                    hookParam.setKey(proxyKey);
                    hookParam.setVhost("__defaultVhost__");
                    hookParam.setEnableHls(true);
                    hookParam.setEnableMp4(true);

                    hookService.onProxyAdded(hookParam, null);

                    log.debug("并发Hook处理完成 - Stream: {}, ProxyKey: {}", stream, proxyKey);
                } catch (Exception e) {
                    log.error("并发Hook处理失败: {}", e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            }, executor))
            .toArray(CompletableFuture[]::new);

        // 等待所有任务完成
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        long processingTime = System.currentTimeMillis() - startTime;

        // 等待异步处理完成
        waitForAsyncOperation(1000);

        // 验证处理结果
        int successCount = 0;
        for (int i = 0; i < concurrentCount; i++) {
            String stream = TEST_PREFIX + "stream_batch_" + i;
            String proxyKey = TEST_PROXY_KEY + "_concurrent_" + i;

            try {
                StreamProxyDO proxy = streamProxyManager.getByAppAndStream(TEST_APP, stream);
                if (proxy != null && proxyKey.equals(proxy.getProxyKey()) && proxy.getOnlineStatus() == 1) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("验证并发Hook处理结果时出现异常: {}", e.getMessage());
            }
        }

        executor.shutdown();

        log.info("并发Hook处理性能 - 总数: {}, 成功: {}, 耗时: {}ms, 平均: {}ms/个",
            concurrentCount, successCount, processingTime, processingTime / concurrentCount);

        // 成功率应该达到90%以上
        assertTrue(successCount >= concurrentCount * 0.9,
            String.format("并发Hook处理成功率不足，期望90%%以上，实际: %.1f%%",
                (double)successCount / concurrentCount * 100));

        log.info("=== 高并发Hook回调处理测试通过 ===");
    }

    @Test
    @DisplayName("03_缓存性能测试")
    public void test03_CachePerformance() throws Exception {
        log.info("=== 开始缓存性能测试 ===");

        // 创建测试代理
        StreamProxyDO testProxy = createTestStreamProxyDO();
        testProxy.setStream(TEST_STREAM + "_cache_perf");

        Long proxyId = streamProxyManager.createStreamProxy(streamProxyManager.doToDto(testProxy));

        // 模拟Hook回调设置缓存数据
        OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
        hookParam.setApp(TEST_APP);
        hookParam.setStream(TEST_STREAM + "_cache_perf");
        hookParam.setUrl(TEST_URL);
        hookParam.setKey(TEST_PROXY_KEY + "_cache_perf");
        hookParam.setVhost("__defaultVhost__");

        hookService.onProxyAdded(hookParam, null);
        waitForAsyncOperation(200);

        // 第一次查询，确保缓存已生成
        streamProxyManager.getById(proxyId);

        int queryCount = 100;

        // 测试缓存查询性能
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < queryCount; i++) {
            StreamProxyDO proxy = streamProxyManager.getById(proxyId);
            assertNotNull(proxy);
            assertEquals(proxyId, proxy.getId());
        }

        long cacheQueryTime = System.currentTimeMillis() - startTime;

        log.info("缓存查询性能 - 查询次数: {}, 总耗时: {}ms, 平均: {}ms/次",
            queryCount, cacheQueryTime, (double)cacheQueryTime / queryCount);

        // 缓存查询平均时间应小于5ms
        assertTrue((double)cacheQueryTime / queryCount < 5,
            "缓存查询性能不符合预期，平均耗时应小于5ms");

        // 测试不同查询方式的性能一致性
        String proxyKey = TEST_PROXY_KEY + "_cache_perf";

        startTime = System.currentTimeMillis();
        for (int i = 0; i < queryCount; i++) {
            StreamProxyDO proxy = streamProxyManager.getByProxyKey(proxyKey);
            assertNotNull(proxy);
        }
        long proxyKeyQueryTime = System.currentTimeMillis() - startTime;

        startTime = System.currentTimeMillis();
        for (int i = 0; i < queryCount; i++) {
            StreamProxyDO proxy = streamProxyManager.getByAppAndStream(TEST_APP, TEST_STREAM + "_cache_perf");
            assertNotNull(proxy);
        }
        long appStreamQueryTime = System.currentTimeMillis() - startTime;

        log.info("不同查询方式性能 - ByProxyKey: {}ms, ByAppStream: {}ms",
            proxyKeyQueryTime, appStreamQueryTime);

        log.info("=== 缓存性能测试通过 ===");
    }

    @Test
    @DisplayName("04_大数据量分页查询性能测试")
    public void test04_LargeDataPageQueryPerformance() throws Exception {
        log.info("=== 开始大数据量分页查询性能测试 ===");

        // 创建大量测试数据
        int largeDataSize = 100;
        List<Long> proxyIds = testDataUtil.createBatchTestProxies(largeDataSize);

        assertEquals(largeDataSize, proxyIds.size());
        log.info("大数据量测试数据准备完成 - 数量: {}", largeDataSize);

        // 测试分页查询性能
        int pageSize = 10;
        int totalPages = (largeDataSize + pageSize - 1) / pageSize;

        long startTime = System.currentTimeMillis();

        for (int page = 1; page <= totalPages; page++) {
            var pageResult = streamProxyManager.getProxyPage(page, pageSize);
            assertNotNull(pageResult);
            assertTrue(pageResult.getRecords().size() <= pageSize);
        }

        long pageQueryTime = System.currentTimeMillis() - startTime;

        log.info("分页查询性能 - 总页数: {}, 页大小: {}, 总耗时: {}ms, 平均: {}ms/页",
            totalPages, pageSize, pageQueryTime, (double)pageQueryTime / totalPages);

        // 分页查询平均时间应小于50ms
        assertTrue((double)pageQueryTime / totalPages < 50,
            "分页查询性能不符合预期，平均耗时应小于50ms");

        log.info("=== 大数据量分页查询性能测试通过 ===");
    }

    @Test
    @DisplayName("05_内存使用和清理测试")
    public void test05_MemoryUsageTest() throws Exception {
        log.info("=== 开始内存使用和清理测试 ===");

        // 记录初始内存使用
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // 创建大量数据
        int dataSize = 200;
        List<Long> proxyIds = testDataUtil.createBatchTestProxies(dataSize);

        // 执行大量查询操作
        for (Long proxyId : proxyIds) {
            streamProxyManager.getById(proxyId);
        }

        // 记录峰值内存使用
        long peakMemory = runtime.totalMemory() - runtime.freeMemory();

        // 清理数据
        testDataUtil.cleanTestProxies();

        // 强制垃圾回收
        System.gc();
        waitForAsyncOperation(1000);

        // 记录清理后内存使用
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();

        log.info("内存使用情况 - 初始: {}MB, 峰值: {}MB, 清理后: {}MB",
            initialMemory / 1024 / 1024,
            peakMemory / 1024 / 1024,
            finalMemory / 1024 / 1024);

        // 验证内存有合理释放（清理后内存应该接近初始内存）
        long memoryIncrease = finalMemory - initialMemory;
        long maxAcceptableIncrease = 50 * 1024 * 1024; // 50MB

        assertTrue(memoryIncrease < maxAcceptableIncrease,
            String.format("内存使用增长过多，增长: %dMB, 最大允许: %dMB",
                memoryIncrease / 1024 / 1024, maxAcceptableIncrease / 1024 / 1024));

        log.info("=== 内存使用和清理测试通过 ===");
    }
}