package io.github.lunasaw.voglander.intergration.wrapper.common;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.common.ZlmWrapperValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WrapperExceptionHandler 单元测试
 * <p>
 * 测试Wrapper层统一异常处理工具类的功能
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@DisplayName("WrapperExceptionHandler 测试")
class WrapperExceptionHandlerTest {

    @Test
    @DisplayName("测试 executeWithExceptionHandling - 成功场景")
    void testExecuteWithExceptionHandling_Success() {
        // Given
        String expectedResult = "success";

        // When
        ResultDTO<String> result = WrapperExceptionHandler.executeWithExceptionHandling(
            () -> expectedResult, "测试操作");

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(expectedResult, result.getData());
        log.info("测试通过 - 成功场景: {}", result);
    }

    @Test
    @DisplayName("测试 executeWithExceptionHandling - 返回null场景")
    void testExecuteWithExceptionHandling_NullResult() {
        // When
        ResultDTO<String> result = WrapperExceptionHandler.executeWithExceptionHandling(
            () -> null, "测试操作");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getMessage().contains("返回结果为空"));
        log.info("测试通过 - 空结果场景: {}", result.getMessage());
    }

    @Test
    @DisplayName("测试 executeWithExceptionHandling - 异常场景")
    void testExecuteWithExceptionHandling_Exception() {
        // Given
        String errorMessage = "模拟异常";

        // When
        ResultDTO<String> result = WrapperExceptionHandler.executeWithExceptionHandling(() -> {
            throw new RuntimeException(errorMessage);
        }, "测试操作");

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertTrue(result.getMessage().contains(errorMessage));
        log.info("测试通过 - 异常场景: {}", result.getMessage());
    }

    @Test
    @DisplayName("测试 validateZlmConnection - 正常场景")
    void testValidateZlmConnection_Valid() {
        // Given
        String host = "localhost:80";
        String secret = "test-secret";

        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> {
            ZlmWrapperValidator.validateZlmConnection(host, secret);
        });
        log.info("测试通过 - ZLM连接参数验证成功");
    }

    @Test
    @DisplayName("测试 validateZlmConnection - Host为空")
    void testValidateZlmConnection_EmptyHost() {
        // Given
        String host = "";
        String secret = "test-secret";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ZlmWrapperValidator.validateZlmConnection(host, secret);
        });

        assertTrue(exception.getMessage().contains("ZLM节点地址"));
        log.info("测试通过 - Host为空异常: {}", exception.getMessage());
    }

    @Test
    @DisplayName("测试 validateRequest - 请求对象为空")
    void testValidateRequest_Null() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            WrapperExceptionHandler.validateRequest(null, "测试请求");
        });

        assertTrue(exception.getMessage().contains("测试请求不能为空"));
        log.info("测试通过 - 请求对象为空异常: {}", exception.getMessage());
    }

    @Test
    @DisplayName("测试 validateAppAndStream - 应用名称为空")
    void testValidateAppAndStream_EmptyApp() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ZlmWrapperValidator.validateAppAndStream("", "test-stream");
        });

        assertTrue(exception.getMessage().contains("应用名称"));
        log.info("测试通过 - 应用名称为空异常: {}", exception.getMessage());
    }

    @Test
    @DisplayName("测试 validateProxyKey - 代理key为空")
    void testValidateProxyKey_Empty() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ZlmWrapperValidator.validateProxyKey("");
        });

        assertTrue(exception.getMessage().contains("代理key"));
        log.info("测试通过 - 代理key为空异常: {}", exception.getMessage());
    }
}