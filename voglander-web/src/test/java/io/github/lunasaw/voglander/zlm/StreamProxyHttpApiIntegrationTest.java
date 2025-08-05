package io.github.lunasaw.voglander.zlm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyVO;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyListResp;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.impl.VoglanderZlmHookServiceImpl;
import io.github.lunasaw.zlm.hook.param.OnProxyAddedHookParam;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * StreamProxy HTTP API集成测试
 * 
 * 测试覆盖：
 * 1. REST API端点调用
 * 2. 请求参数验证
 * 3. 响应格式验证
 * 4. HTTP状态码验证
 * 5. API与Hook回调联动
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@DisplayName("StreamProxy HTTP API集成测试")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class StreamProxyHttpApiIntegrationTest extends BaseStreamProxyIntegrationTest {

    @Autowired
    private VoglanderZlmHookServiceImpl hookService;

    @BeforeEach
    public void setUpTest() {
        // 确保每个测试开始前数据库连接是干净的
        cleanTestData(); // 先清理一次
        waitForAsyncOperation(50);
        log.info("开始执行StreamProxy HTTP API测试");
    }

    @AfterEach
    public void tearDownTest() {
        // 确保测试结束后清理资源
        waitForAsyncOperation(50);
        log.info("StreamProxy HTTP API测试完成");
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("01_创建拉流代理API测试")
    public void test01_CreateStreamProxyApi() throws Exception {
        log.info("=== 开始创建拉流代理API测试 ===");

        // 构建创建请求
        StreamProxyCreateReq createReq = new StreamProxyCreateReq();
        createReq.setApp(TEST_APP);
        createReq.setStream(TEST_STREAM + "_api");
        createReq.setUrl(TEST_URL);
        createReq.setDescription(TEST_DESCRIPTION + "_api");
        createReq.setEnabled(true);

        HttpEntity<StreamProxyCreateReq> requestEntity = new HttpEntity<>(createReq, createJsonHeaders());

        // 调用创建API
        ResponseEntity<AjaxResult> response = restTemplate.postForEntity(
            "/api/v1/proxy/create",
            requestEntity,
            AjaxResult.class);

        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
        assertNotNull(response.getBody().getData());

        Long proxyId = ((Number)response.getBody().getData()).longValue();
        log.info("API测试 - 创建拉流代理成功，ID: {}", proxyId);

        // 验证数据库记录（使用重试机制）
        StreamProxyDO savedProxy = executeWithRetry(
            () -> streamProxyManager.getById(proxyId),
            "获取拉流代理记录");
        assertNotNull(savedProxy);
        assertEquals(TEST_APP, savedProxy.getApp());
        assertEquals(TEST_STREAM + "_api", savedProxy.getStream());
        assertEquals(TEST_URL, savedProxy.getUrl());

        log.info("=== 创建拉流代理API测试通过 ===");
    }

    @Test
    @DisplayName("02_根据ID获取代理API测试")
    public void test02_GetByIdApi() throws Exception {
        log.info("=== 开始根据ID获取代理API测试 ===");

        // 先创建一个代理
        StreamProxyDO testProxy = createTestStreamProxyDO();
        testProxy.setStream(TEST_STREAM + "_getid");
        testProxy.setProxyKey(TEST_PROXY_KEY + "_getid");
        testProxy.setOnlineStatus(1);

        Long proxyId = streamProxyManager.createStreamProxy(streamProxyManager.doToDto(testProxy));

        // 模拟Hook回调设置proxy key和在线状态
        OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
        hookParam.setApp(TEST_APP);
        hookParam.setStream(TEST_STREAM + "_getid");
        hookParam.setUrl(TEST_URL);
        hookParam.setKey(TEST_PROXY_KEY + "_getid");
        hookParam.setVhost("__defaultVhost__");

        hookService.onProxyAdded(hookParam, null);
        waitForAsyncOperation(200);

        // 调用获取API
        ResponseEntity<AjaxResult> response = restTemplate.getForEntity(
            "/api/v1/proxy/get/" + proxyId,
            AjaxResult.class);

        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());

        // 验证返回的数据
        Object data = response.getBody().getData();
        assertNotNull(data);
        log.info("API测试 - 获取代理成功，数据: {}", data);

        log.info("=== 根据ID获取代理API测试通过 ===");
    }

    @Test
    @DisplayName("03_根据ProxyKey获取代理API测试")
    public void test03_GetByProxyKeyApi() throws Exception {
        log.info("=== 开始根据ProxyKey获取代理API测试 ===");

        String testProxyKey = TEST_PROXY_KEY + "_getkey";

        // 创建代理并模拟Hook回调
        StreamProxyDO testProxy = createTestStreamProxyDO();
        testProxy.setStream(TEST_STREAM + "_getkey");

        Long proxyId = streamProxyManager.createStreamProxy(streamProxyManager.doToDto(testProxy));

        OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
        hookParam.setApp(TEST_APP);
        hookParam.setStream(TEST_STREAM + "_getkey");
        hookParam.setUrl(TEST_URL);
        hookParam.setKey(testProxyKey);
        hookParam.setVhost("__defaultVhost__");

        hookService.onProxyAdded(hookParam, null);
        waitForAsyncOperation(200);

        // 调用根据ProxyKey获取的API
        ResponseEntity<AjaxResult> response = restTemplate.getForEntity(
            "/api/v1/proxy/getByKey/" + testProxyKey,
            AjaxResult.class);

        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
        assertNotNull(response.getBody().getData());

        log.info("API测试 - 根据ProxyKey获取代理成功");

        log.info("=== 根据ProxyKey获取代理API测试通过 ===");
    }

    @Test
    @DisplayName("04_分页查询代理API测试")
    public void test04_PageQueryApi() throws Exception {
        log.info("=== 开始分页查询代理API测试 ===");

        // 创建多个测试代理
        int createCount = 3;
        for (int i = 0; i < createCount; i++) {
            StreamProxyDO testProxy = createTestStreamProxyDO();
            testProxy.setStream(TEST_STREAM + "_page_" + i);
            testProxy.setDescription(TEST_DESCRIPTION + "_page_" + i);

            Long proxyId = streamProxyManager.createStreamProxy(streamProxyManager.doToDto(testProxy));

            // 模拟部分代理的Hook回调
            if (i % 2 == 0) {
                OnProxyAddedHookParam hookParam = new OnProxyAddedHookParam();
                hookParam.setApp(TEST_APP);
                hookParam.setStream(TEST_STREAM + "_page_" + i);
                hookParam.setUrl(TEST_URL);
                hookParam.setKey(TEST_PROXY_KEY + "_page_" + i);

                hookService.onProxyAdded(hookParam, null);
            }
        }

        waitForAsyncOperation(300);

        // 调用分页查询API
        ResponseEntity<AjaxResult> response = restTemplate.getForEntity(
            "/api/v1/proxy/page?page=1&size=10",
            AjaxResult.class);

        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());
        assertNotNull(response.getBody().getData());

        log.info("API测试 - 分页查询代理成功，响应数据: {}", response.getBody().getData());

        log.info("=== 分页查询代理API测试通过 ===");
    }

    @Test
    @DisplayName("05_更新代理状态API测试")
    public void test05_UpdateStatusApi() throws Exception {
        log.info("=== 开始更新代理状态API测试 ===");

        // 创建测试代理
        StreamProxyDO testProxy = createTestStreamProxyDO();
        testProxy.setStream(TEST_STREAM + "_update");
        testProxy.setStatus(1); // 初始启用状态

        Long proxyId = streamProxyManager.createStreamProxy(streamProxyManager.doToDto(testProxy));

        // 调用更新状态API
        ResponseEntity<AjaxResult> response = restTemplate.exchange(
            "/api/v1/proxy/updateStatus/" + proxyId + "?status=0",
            org.springframework.http.HttpMethod.PUT,
            null,
            AjaxResult.class);

        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());

        // 验证数据库状态已更新
        StreamProxyDO updatedProxy = streamProxyManager.getById(proxyId);
        assertNotNull(updatedProxy);
        assertEquals(0, updatedProxy.getStatus()); // 应该已更新为禁用状态

        log.info("API测试 - 更新代理状态成功");

        log.info("=== 更新代理状态API测试通过 ===");
    }

    @Test
    @DisplayName("06_删除代理API测试")
    public void test06_DeleteApi() throws Exception {
        log.info("=== 开始删除代理API测试 ===");

        // 创建测试代理
        StreamProxyDO testProxy = createTestStreamProxyDO();
        testProxy.setStream(TEST_STREAM + "_delete");

        Long proxyId = streamProxyManager.createStreamProxy(streamProxyManager.doToDto(testProxy));

        // 确认代理存在
        StreamProxyDO existingProxy = streamProxyManager.getById(proxyId);
        assertNotNull(existingProxy);

        // 调用删除API
        ResponseEntity<AjaxResult> response = restTemplate.exchange(
            "/api/v1/proxy/delete/" + proxyId,
            org.springframework.http.HttpMethod.DELETE,
            null,
            AjaxResult.class);

        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCode());

        // 验证代理已删除
        StreamProxyDO deletedProxy = streamProxyManager.getById(proxyId);
        assertNull(deletedProxy);

        log.info("API测试 - 删除代理成功");

        log.info("=== 删除代理API测试通过 ===");
    }

    @Test
    @DisplayName("07_API参数验证测试")
    public void test07_ApiValidationTest() throws Exception {
        log.info("=== 开始API参数验证测试 ===");

        // 测试创建时缺少必需参数
        StreamProxyCreateReq invalidReq = new StreamProxyCreateReq();
        // 故意不设置app和stream
        invalidReq.setUrl(TEST_URL);

        HttpEntity<StreamProxyCreateReq> requestEntity = new HttpEntity<>(invalidReq, createJsonHeaders());

        ResponseEntity<AjaxResult> response = restTemplate.postForEntity(
            "/api/v1/proxy/create",
            requestEntity,
            AjaxResult.class);

        // 应该返回400错误或业务错误码
        assertTrue(response.getStatusCode() == HttpStatus.BAD_REQUEST ||
            (response.getBody() != null && response.getBody().getCode() != 0));

        log.info("API测试 - 参数验证测试通过");

        // 测试获取不存在的代理
        ResponseEntity<AjaxResult> notFoundResponse = restTemplate.getForEntity(
            "/api/v1/proxy/get/99999",
            AjaxResult.class);

        // 应该返回错误信息
        assertNotNull(notFoundResponse.getBody());
        assertNotEquals(0, notFoundResponse.getBody().getCode());

        log.info("=== API参数验证测试通过 ===");
    }
}