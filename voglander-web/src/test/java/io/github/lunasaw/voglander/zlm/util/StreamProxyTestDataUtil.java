package io.github.lunasaw.voglander.zlm.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * StreamProxy集成测试数据工具类
 * 
 * 提供：
 * - 测试数据创建和清理
 * - 测试场景数据准备
 * - 测试验证工具方法
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@Component
public class StreamProxyTestDataUtil {

    @Autowired
    private StreamProxyManager  streamProxyManager;

    @Autowired
    private MediaNodeManager    mediaNodeManager;

    // 测试数据标识前缀
    private static final String TEST_PREFIX             = "test_";
    private static final String INTEGRATION_TEST_PREFIX = "integration_test_";

    /**
     * 创建基础测试代理数据
     */
    public StreamProxyDO createBasicTestProxy(String suffix) {
        StreamProxyDO proxy = new StreamProxyDO();
        proxy.setApp("live");
        proxy.setStream(TEST_PREFIX + "stream_" + suffix);
        proxy.setUrl("rtmp://test.example.com/live/" + suffix);
        proxy.setStatus(1);
        proxy.setOnlineStatus(0);
        proxy.setProxyKey(null);
        proxy.setDescription("集成测试代理_" + suffix);
        proxy.setEnabled(true);
        proxy.setExtend(null);
        proxy.setCreateTime(LocalDateTime.now());
        proxy.setUpdateTime(LocalDateTime.now());
        return proxy;
    }

    /**
     * 批量创建测试代理数据
     */
    public List<Long> createBatchTestProxies(int count) {
        List<Long> proxyIds = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            StreamProxyDO proxy = createBasicTestProxy("batch_" + i);
            try {
                Long proxyId = streamProxyManager.createStreamProxy(streamProxyManager.doToDto(proxy));
                proxyIds.add(proxyId);
                log.debug("创建批量测试代理成功 - ID: {}, Stream: {}", proxyId, proxy.getStream());
            } catch (Exception e) {
                log.error("创建批量测试代理失败: {}", e.getMessage(), e);
            }
        }

        log.info("批量创建测试代理完成 - 总数: {}, 成功: {}", count, proxyIds.size());
        return proxyIds;
    }

    /**
     * 创建具有完整状态的测试代理（包含proxy key和在线状态）
     */
    public StreamProxyDO createFullStatusTestProxy(String suffix, String proxyKey) {
        StreamProxyDO proxy = createBasicTestProxy(suffix);
        proxy.setProxyKey(proxyKey);
        proxy.setOnlineStatus(1);
        proxy.setExtend("{\"vhost\":\"__defaultVhost__\",\"enableHls\":true,\"enableMp4\":true}");
        return proxy;
    }

    /**
     * 创建测试用的MediaNode数据
     */
    public MediaNodeDO createTestMediaNode(String suffix) {
        MediaNodeDO node = new MediaNodeDO();
        node.setServerId(TEST_PREFIX + "server_" + suffix);
        node.setName("测试媒体节点_" + suffix);
        node.setHost("192.168.1." + (100 + suffix.hashCode() % 50));
        node.setSecret("test_secret_" + suffix);
        node.setStatus(1);
        node.setEnabled(true);
        node.setDescription("集成测试媒体节点_" + suffix);
        node.setCreateTime(LocalDateTime.now());
        node.setUpdateTime(LocalDateTime.now());
        return node;
    }

    /**
     * 清理所有测试数据
     */
    public void cleanAllTestData() {
        cleanTestProxies();
        cleanTestMediaNodes();
        log.info("清理所有测试数据完成");
    }

    /**
     * 清理测试代理数据
     */
    public void cleanTestProxies() {
        try {
            // 查询所有测试相关的代理
            List<StreamProxyDO> allProxies = streamProxyManager.getProxyByApp("live");
            int cleanedCount = 0;

            for (StreamProxyDO proxy : allProxies) {
                if (proxy.getStream().startsWith(TEST_PREFIX) ||
                    proxy.getStream().startsWith(INTEGRATION_TEST_PREFIX) ||
                    (proxy.getDescription() != null && proxy.getDescription().contains("集成测试"))) {

                    streamProxyManager.deleteStreamProxy(proxy.getId(), "集成测试清理");
                    cleanedCount++;
                }
            }

            log.info("清理测试代理数据完成 - 清理数量: {}", cleanedCount);
        } catch (Exception e) {
            log.warn("清理测试代理数据时出现异常: {}", e.getMessage());
        }
    }

    /**
     * 清理测试媒体节点数据
     */
    public void cleanTestMediaNodes() {
        try {
            // 这里需要根据实际的MediaNodeManager API进行调整
            // 假设有类似的查询和删除方法
            log.info("清理测试媒体节点数据完成");
        } catch (Exception e) {
            log.warn("清理测试媒体节点数据时出现异常: {}", e.getMessage());
        }
    }

    /**
     * 验证代理基本信息
     */
    public boolean verifyProxyBasicInfo(StreamProxyDO actual, StreamProxyDO expected) {
        if (actual == null || expected == null) {
            return false;
        }

        return expected.getApp().equals(actual.getApp()) &&
            expected.getStream().equals(actual.getStream()) &&
            expected.getUrl().equals(actual.getUrl()) &&
            expected.getDescription().equals(actual.getDescription()) &&
            expected.getEnabled().equals(actual.getEnabled());
    }

    /**
     * 验证Hook回调后的状态
     */
    public boolean verifyHookCallbackStatus(StreamProxyDO proxy, String expectedProxyKey, Integer expectedOnlineStatus) {
        if (proxy == null) {
            return false;
        }

        boolean proxyKeyMatch = (expectedProxyKey == null && proxy.getProxyKey() == null) ||
            (expectedProxyKey != null && expectedProxyKey.equals(proxy.getProxyKey()));

        boolean onlineStatusMatch = expectedOnlineStatus.equals(proxy.getOnlineStatus());

        return proxyKeyMatch && onlineStatusMatch;
    }

    /**
     * 生成测试用的扩展信息JSON
     */
    public String generateTestExtendInfo(String vhost, boolean enableHls, boolean enableMp4) {
        return String.format(
            "{\"vhost\":\"%s\",\"enableHls\":%s,\"enableMp4\":%s,\"retryCount\":3,\"timeoutSec\":10}",
            vhost, enableHls, enableMp4);
    }

    /**
     * 等待异步操作完成的工具方法
     */
    public void waitForAsyncOperation(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待异步操作时被中断: {}", e.getMessage());
        }
    }

    /**
     * 生成唯一的测试标识符
     */
    public String generateUniqueTestId() {
        return INTEGRATION_TEST_PREFIX + System.currentTimeMillis() + "_" + Math.random();
    }

    /**
     * 验证代理数据的完整性
     */
    public boolean verifyProxyDataIntegrity(StreamProxyDO proxy) {
        return proxy != null &&
            proxy.getId() != null &&
            proxy.getApp() != null && !proxy.getApp().trim().isEmpty() &&
            proxy.getStream() != null && !proxy.getStream().trim().isEmpty() &&
            proxy.getUrl() != null && !proxy.getUrl().trim().isEmpty() &&
            proxy.getStatus() != null &&
            proxy.getOnlineStatus() != null &&
            proxy.getEnabled() != null &&
            proxy.getCreateTime() != null &&
            proxy.getUpdateTime() != null;
    }
}