package io.github.lunasaw.voglander.intergration.wrapper.common;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import com.luna.common.dto.constant.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.function.Supplier;

/**
 * Wrapper层统一异常处理工具类
 * <p>
 * 按照Wrapper层设计原则：只处理参数验证和异常捕获，业务逻辑在Service层实现
 * 返回时：成功返回具体模型，异常返回null，使用断言直接抛出异常进行必要参数验证
 * </p>
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
public class WrapperExceptionHandler {

    /**
     * Wrapper层统一异常处理模板方法
     * <p>
     * 执行业务逻辑并处理异常，成功返回具体模型，异常返回null
     * </p>
     *
     * @param businessLogic 业务逻辑执行器
     * @param operationDesc 操作描述，用于日志记录
     * @param <T> 返回数据类型
     * @return ResultDTO包装的结果，成功时data为具体模型，异常时data为null
     */
    public static <T> ResultDTO<T> executeWithExceptionHandling(
        Supplier<T> businessLogic,
        String operationDesc) {

        try {
            log.debug("开始执行{}", operationDesc);

            // 执行业务逻辑
            T result = businessLogic.get();

            if (result == null) {
                log.warn("{}执行完成，但返回结果为空", operationDesc);
                return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, operationDesc + "返回结果为空", null);
            }

            log.debug("{}执行成功", operationDesc);
            return ResultDTOUtils.success(result);

        } catch (Exception e) {
            log.error("{}执行异常: {}", operationDesc, e.getMessage(), e);
            return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, operationDesc + "异常: " + e.getMessage(), null);
        }
    }

    /**
     * 通用参数验证工具方法 - 验证请求对象不为空
     * 
     * @param request 请求对象
     * @param requestDesc 请求描述
     * @throws IllegalArgumentException 当请求对象为空时
     */
    public static void validateRequest(Object request, String requestDesc) {
        Assert.notNull(request, requestDesc + "不能为空");
    }

    /**
     * 通用参数验证工具方法 - 验证字符串不为空
     * 
     * @param value 字符串值
     * @param fieldDesc 字段描述
     * @throws IllegalArgumentException 当字符串为空时
     */
    public static void validateNotEmpty(String value, String fieldDesc) {
        Assert.hasText(value, fieldDesc + "不能为空");
    }

    /**
     * 通用参数验证工具方法 - 验证对象不为空
     * 
     * @param value 对象值
     * @param fieldDesc 字段描述
     * @throws IllegalArgumentException 当对象为空时
     */
    public static void validateNotNull(Object value, String fieldDesc) {
        Assert.notNull(value, fieldDesc + "不能为空");
    }
}