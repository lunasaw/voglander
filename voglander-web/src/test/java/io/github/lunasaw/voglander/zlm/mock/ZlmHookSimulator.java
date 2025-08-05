package io.github.lunasaw.voglander.zlm.mock;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * ZLM Hook回调模拟器
 * 用于在集成测试中模拟ZLMediaKit的Hook回调
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@Component
public class ZlmHookSimulator {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 模拟拉流代理添加成功的Hook回调
     *
     * @param baseUrl 应用基础URL
     * @param app 应用名称
     * @param stream 流ID
     * @param url 拉流地址
     * @param proxyKey 代理key
     * @return 异步执行结果
     */
    public CompletableFuture<Boolean> simulateProxyAddedHook(String baseUrl, String app, String stream, String url, String proxyKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟异步延迟，符合真实场景
                TimeUnit.MILLISECONDS.sleep(100);

                String hookUrl = baseUrl + "/zlm/hook/on_proxy_added";

                // 构建Hook参数
                OnProxyAddedHookRequest hookRequest = OnProxyAddedHookRequest.builder()
                    .mediaServerId("test-zlm-server")
                    .app(app)
                    .stream(stream)
                    .url(url)
                    .key(proxyKey)
                    .vhost("__defaultVhost__")
                    .retryCount(3)
                    .rtpType(0)
                    .timeoutSec(10)
                    .enableHls(true)
                    .enableHlsFmp4(false)
                    .enableMp4(true)
                    .enableRtsp(true)
                    .enableRtmp(true)
                    .enableTs(true)
                    .enableFmp4(false)
                    .build();

                // 发送Hook回调请求
                ResponseEntity<String> response = restTemplate.postForEntity(hookUrl, hookRequest, String.class);

                boolean success = response.getStatusCode() == HttpStatus.OK;
                log.info("Hook回调模拟完成 - URL: {}, 状态: {}, 结果: {}", hookUrl, response.getStatusCode(), success);

                return success;
            } catch (Exception e) {
                log.error("Hook回调模拟失败: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * 模拟服务器启动Hook回调
     *
     * @param baseUrl 应用基础URL
     * @param serverId 服务器ID
     * @param apiSecret API密钥
     * @param httpPort HTTP端口
     * @return 异步执行结果
     */
    public CompletableFuture<Boolean> simulateServerStartedHook(String baseUrl, String serverId, String apiSecret, String httpPort) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(50);

                String hookUrl = baseUrl + "/zlm/hook/on_server_started";

                // 构建服务器启动Hook参数
                OnServerStartedHookRequest hookRequest = OnServerStartedHookRequest.builder()
                    .mediaServerId(serverId)
                    .apiSecret(apiSecret)
                    .httpPort(httpPort)
                    .build();

                ResponseEntity<String> response = restTemplate.postForEntity(hookUrl, hookRequest, String.class);

                boolean success = response.getStatusCode() == HttpStatus.OK;
                log.info("服务器启动Hook回调模拟完成 - 服务器ID: {}, 结果: {}", serverId, success);

                return success;
            } catch (Exception e) {
                log.error("服务器启动Hook回调模拟失败: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * 模拟流状态变化Hook回调
     *
     * @param baseUrl 应用基础URL
     * @param app 应用名称
     * @param stream 流ID
     * @param regist 注册状态
     * @return 异步执行结果
     */
    public CompletableFuture<Boolean> simulateStreamChangedHook(String baseUrl, String app, String stream, boolean regist) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(50);

                String hookUrl = baseUrl + "/zlm/hook/on_stream_changed";

                OnStreamChangedHookRequest hookRequest = OnStreamChangedHookRequest.builder()
                    .mediaServerId("test-zlm-server")
                    .app(app)
                    .stream(stream)
                    .regist(regist)
                    .schema("rtmp")
                    .vhost("__defaultVhost__")
                    .build();

                ResponseEntity<String> response = restTemplate.postForEntity(hookUrl, hookRequest, String.class);

                boolean success = response.getStatusCode() == HttpStatus.OK;
                log.info("流状态变化Hook回调模拟完成 - 流: {}/{}, 状态: {}, 结果: {}", app, stream, regist, success);

                return success;
            } catch (Exception e) {
                log.error("流状态变化Hook回调模拟失败: {}", e.getMessage(), e);
                return false;
            }
        });
    }
}