package io.github.lunasaw.voglander.zlm;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lunasaw.zlm.entity.StreamKey;
import io.github.lunasaw.zlm.entity.StreamProxyItem;
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
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
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
    @DisplayName("02_根据ID获取代理API测试")
    public void test02_GetByIdApi() throws Exception {
        log.info("=== 开始根据ID获取代理API测试 ===");

        // 先创建一个代理
        StreamProxyDTO testProxy = createTestStreamProxyDTO();
        testProxy.setStream(TEST_STREAM + "_getid");
        testProxy.setProxyKey(TEST_PROXY_KEY + "_getid");
        testProxy.setOnlineStatus(1);

        Long proxyId = streamProxyManager.createStreamProxy(testProxy);

        // 模拟Hook回调设置proxy key和在线状态
        StreamProxyItem hookParam = new StreamProxyItem();
        hookParam.setApp(TEST_APP);
        hookParam.setStream(TEST_STREAM + "_getid");
        hookParam.setUrl(TEST_URL);
        StreamKey streamKey = new StreamKey();
        streamKey.setKey(TEST_PROXY_KEY + "_getid");

        hookService.onProxyAdded(hookParam, streamKey, null);
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