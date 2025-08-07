package io.github.lunasaw.voglander.intergration.wrapper.zlm.service.impl;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto.StreamProxyRequest;
import io.github.lunasaw.zlm.entity.StreamKey;
import io.github.lunasaw.zlm.entity.StreamProxyItem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StreamProxyZlmWrapperServiceImpl 单元测试
 * <p>
 * 测试Wrapper层的参数验证和异常处理逻辑，不依赖外部ZLM服务
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayName("StreamProxyZlmWrapperService 测试")
class StreamProxyZlmWrapperServiceImplTest {

    @InjectMocks
    private StreamProxyZlmWrapperServiceImpl streamProxyZlmWrapperService;

    @Test
    @DisplayName("测试 addStreamProxy 参数验证 - 请求为空")
    void testAddStreamProxy_NullRequest() {
        // Given
        StreamProxyRequest request = null;

        // When
        ResultDTO<StreamKey> result = streamProxyZlmWrapperService.addStreamProxy(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getMessage().contains("请求参数"));
        log.info("测试通过 - 空请求参数验证: {}", result.getMessage());
    }

    @Test
    @DisplayName("测试 addStreamProxy 参数验证 - Host为空")
    void testAddStreamProxy_EmptyHost() {
        // Given
        StreamProxyItem streamProxyItem = new StreamProxyItem();
        streamProxyItem.setApp("live");
        streamProxyItem.setStream("test");
        streamProxyItem.setUrl("rtmp://example.com/live/test");

        StreamProxyRequest request = StreamProxyRequest.builder()
            .host("") // 空的host
            .secret("secret")
            .streamProxyItem(streamProxyItem)
            .build();

        // When
        ResultDTO<StreamKey> result = streamProxyZlmWrapperService.addStreamProxy(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getMessage().contains("ZLM节点地址"));
        log.info("测试通过 - 空Host参数验证: {}", result.getMessage());
    }

    @Test
    @DisplayName("测试 addStreamProxy 参数验证 - StreamProxyItem为空")
    void testAddStreamProxy_NullStreamProxyItem() {
        // Given
        StreamProxyRequest request = StreamProxyRequest.builder()
            .host("localhost:80")
            .secret("secret")
            .streamProxyItem(null) // 空的streamProxyItem
            .build();

        // When
        ResultDTO<StreamKey> result = streamProxyZlmWrapperService.addStreamProxy(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getMessage().contains("流代理配置"));
        log.info("测试通过 - 空StreamProxyItem参数验证: {}", result.getMessage());
    }

    @Test
    @DisplayName("测试 addStreamProxy 参数验证 - 应用名称为空")
    void testAddStreamProxy_EmptyApp() {
        // Given
        StreamProxyItem streamProxyItem = new StreamProxyItem();
        streamProxyItem.setApp(""); // 空的app
        streamProxyItem.setStream("test");
        streamProxyItem.setUrl("rtmp://example.com/live/test");

        StreamProxyRequest request = StreamProxyRequest.builder()
            .host("localhost:80")
            .secret("secret")
            .streamProxyItem(streamProxyItem)
            .build();

        // When
        ResultDTO<StreamKey> result = streamProxyZlmWrapperService.addStreamProxy(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getMessage().contains("应用名称"));
        log.info("测试通过 - 空应用名称参数验证: {}", result.getMessage());
    }

    @Test
    @DisplayName("测试 addStreamProxy 参数验证 - 拉流地址为空")
    void testAddStreamProxy_EmptyUrl() {
        // Given
        StreamProxyItem streamProxyItem = new StreamProxyItem();
        streamProxyItem.setApp("live");
        streamProxyItem.setStream("test");
        streamProxyItem.setUrl(""); // 空的url

        StreamProxyRequest request = StreamProxyRequest.builder()
            .host("localhost:80")
            .secret("secret")
            .streamProxyItem(streamProxyItem)
            .build();

        // When
        ResultDTO<StreamKey> result = streamProxyZlmWrapperService.addStreamProxy(request);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getMessage().contains("拉流地址"));
        log.info("测试通过 - 空拉流地址参数验证: {}", result.getMessage());
    }
}