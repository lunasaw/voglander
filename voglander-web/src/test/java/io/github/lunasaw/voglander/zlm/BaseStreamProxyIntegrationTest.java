package io.github.lunasaw.voglander.zlm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.zlm.mock.ZlmServiceMock;
import io.github.lunasaw.voglander.zlm.mock.ZlmHookSimulator;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * StreamProxy集成测试基类
 * 
 * 提供：
 * - ZLM外部服务Mock
 * - Hook回调模拟器
 * - 测试数据管理
 * - 公共测试工具方法
 * 
 * 注意：HTTP API集成测试不使用@Transactional，避免锁等待超时问题
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.web.ApplicationWeb.class,
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ZlmServiceMock.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // 禁用不必要的组件以加快测试速度
    "local.sip.server.enabled=false",
    "local.sip.client.enabled=false",
    "sip.enable=false",
    // ZLM测试配置
    "zlm.enable=true",
    "zlm.hook-enable=true",
    "zlm.hook.admin.enable=true",
    // 数据库配置 - 使用与application-test.yml一致的配置
    "spring.datasource.dynamic.datasource.master.driver-class-name=org.sqlite.JDBC",
    "spring.datasource.dynamic.datasource.master.url=jdbc:sqlite:test-app.db",
    "spring.datasource.dynamic.primary=master",
    "spring.datasource.dynamic.strict=false",
    // MyBatis Plus配置
    "mybatis-plus.check-config-location=true",
    "mybatis-plus.configuration.cache-enabled=true",
    "mybatis-plus.configuration.use-generated-keys=true",
    "mybatis-plus.configuration.default-executor-type=simple",
    "mybatis-plus.configuration.map-underscore-to-camel-case=true",
    // 缓存配置
    "spring.cache.type=simple",
    // 组件扫描配置
    "spring.main.allow-bean-definition-overriding=true",
    // 日志配置
    "logging.level.io.github.lunasaw.voglander.zlm=DEBUG",
    "logging.level.io.github.lunasaw.voglander.intergration=DEBUG",
    "logging.level.io.github.lunasaw.voglander.manager.manager.StreamProxyManager=DEBUG"
})
public abstract class BaseStreamProxyIntegrationTest {

    @LocalServerPort
    protected int                 port;

    @Autowired
    protected StreamProxyManager  streamProxyManager;

    @Autowired
    protected ZlmHookSimulator    hookSimulator;

    @Autowired
    protected TestRestTemplate    restTemplate;

    protected String              baseUrl;

    // 测试常量
    protected static final String TEST_APP         = "live";
    protected static final String TEST_STREAM      = "test-stream";
    protected static final String TEST_URL         = "rtmp://live.example.com/live/test";
    protected static final String TEST_PROXY_KEY   = "test-proxy-key-123";
    protected static final String TEST_DESCRIPTION = "集成测试拉流代理";
    protected static final String TEST_SERVER_ID   = "test-zlm-server";
    protected static final String TEST_API_SECRET  = "test-api-secret";
    protected static final String TEST_HTTP_PORT   = "8080";

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port;
        log.info("StreamProxy集成测试环境初始化 - 基础URL: {}", baseUrl);
        log.debug("BaseStreamProxyIntegrationTest setUp - HTTP API integration test initialized");
        cleanTestData();
    }

    @AfterEach
    public void tearDown() {
        cleanTestData();
        log.info("StreamProxy集成测试环境清理完成");
    }

    /**
     * 清理测试数据
     * 由于HTTP API集成测试不使用@Transactional，需要手动清理数据避免UNIQUE约束冲突
     */
    protected void cleanTestData() {
        try {
            // 清理所有测试相关的代理数据
            StreamProxyDTO queryDTO = new StreamProxyDTO();
            queryDTO.setApp(TEST_APP);
            queryDTO.setStream(TEST_STREAM);

            StreamProxyDTO existingProxy = streamProxyManager.get(queryDTO);
            if (existingProxy != null) {
                streamProxyManager.deleteStreamProxyById(existingProxy.getId(), "集成测试清理");
            }

            // 额外清理：删除所有可能残留的测试stream
            String[] testStreamPatterns = {
                TEST_STREAM + "_api",
                TEST_STREAM + "_getid",
                TEST_STREAM + "_getkey",
                TEST_STREAM + "_update",
                TEST_STREAM + "_delete"
            };

            for (String streamPattern : testStreamPatterns) {
                try {
                    StreamProxyDTO cleanupQueryDTO = new StreamProxyDTO();
                    cleanupQueryDTO.setApp(TEST_APP);
                    cleanupQueryDTO.setStream(streamPattern);

                    StreamProxyDTO cleanupProxy = streamProxyManager.get(cleanupQueryDTO);
                    if (cleanupProxy != null) {
                        streamProxyManager.deleteStreamProxyById(cleanupProxy.getId(), "集成测试清理-模式匹配");
                    }
                } catch (Exception ignored) {
                    // 不存在时忽略
                }
            }

            // 清理分页测试数据
            for (int i = 0; i < 10; i++) {
                try {
                    StreamProxyDTO pageQueryDTO = new StreamProxyDTO();
                    pageQueryDTO.setApp(TEST_APP);
                    pageQueryDTO.setStream(TEST_STREAM + "_page_" + i);

                    StreamProxyDTO pageProxy = streamProxyManager.get(pageQueryDTO);
                    if (pageProxy != null) {
                        streamProxyManager.deleteStreamProxyById(pageProxy.getId(), "集成测试清理-分页数据");
                    }
                } catch (Exception ignored) {
                    // 不存在时忽略
                }
            }

            log.debug("测试数据清理完成");
        } catch (Exception e) {
            log.warn("清理测试数据时出现异常: {}", e.getMessage());
        }
    }

    /**
     * 创建测试用的StreamProxyDTO对象
     */
    protected StreamProxyDTO createTestStreamProxyDTO() {
        StreamProxyDTO streamProxy = new StreamProxyDTO();
        streamProxy.setApp(TEST_APP);
        streamProxy.setStream(TEST_STREAM);
        streamProxy.setUrl(TEST_URL);
        streamProxy.setStatus(1);
        streamProxy.setOnlineStatus(0);
        streamProxy.setProxyKey(null); // 初始时没有proxy key
        streamProxy.setDescription(TEST_DESCRIPTION);
        streamProxy.setEnabled(true);
        streamProxy.setExtend(null);
        streamProxy.setCreateTime(LocalDateTime.now());
        streamProxy.setUpdateTime(LocalDateTime.now());
        return streamProxy;
    }

    /**
     * 验证StreamProxyDTO对象的基本信息
     */
    protected void verifyStreamProxyBasicInfo(StreamProxyDTO actual, StreamProxyDTO expected) {
        assertNotNull(actual);
        assertEquals(expected.getApp(), actual.getApp());
        assertEquals(expected.getStream(), actual.getStream());
        assertEquals(expected.getUrl(), actual.getUrl());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getEnabled(), actual.getEnabled());
    }

    /**
     * 验证Hook回调后的状态更新
     */
    protected void verifyHookCallbackResult(StreamProxyDTO proxy, String expectedProxyKey, Integer expectedOnlineStatus) {
        assertNotNull(proxy);
        assertEquals(expectedProxyKey, proxy.getProxyKey());
        assertEquals(expectedOnlineStatus, proxy.getOnlineStatus());
        assertNotNull(proxy.getExtend());
    }

    /**
     * 等待异步操作完成的工具方法
     */
    protected void waitForAsyncOperation(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待异步操作时被中断: {}", e.getMessage());
        }
    }

    /**
     * 执行数据库操作，带重试机制处理SQLite锁定
     */
    protected <T> T executeWithRetry(java.util.function.Supplier<T> operation, String operationName) {
        int maxRetries = 3;
        long waitTime = 100; // 起始等待时间(ms)

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("database is locked") && attempt < maxRetries) {
                    log.warn("SQLite数据库锁定，{}操作重试 {}/{}: {}", operationName, attempt, maxRetries, e.getMessage());
                    try {
                        Thread.sleep(waitTime);
                        waitTime *= 2; // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("操作被中断", ie);
                    }
                } else {
                    throw new RuntimeException(operationName + "操作失败", e);
                }
            }
        }
        throw new RuntimeException(operationName + "操作在" + maxRetries + "次重试后仍然失败");
    }
}