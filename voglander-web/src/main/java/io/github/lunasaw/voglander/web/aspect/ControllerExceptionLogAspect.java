package io.github.lunasaw.voglander.web.aspect;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller层异常日志切面
 * 用于记录所有Controller方法执行过程中的异常信息，包括入参数和异常堆栈
 * 捕获异常并封装成AjaxResult返回，避免异常继续传播
 *
 * @author luna
 */
@Aspect
@Component
@Slf4j
public class ControllerExceptionLogAspect {

    /**
     * 环绕通知，拦截所有Controller层的方法
     * 当方法执行异常时，记录详细的异常日志信息，并封装成AjaxResult返回
     */
    @Around("execution(* io.github.lunasaw.voglander.web.api..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // 正常执行方法
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 异常时记录详细日志
            logException(joinPoint, throwable);

            // 对于认证相关异常，直接抛出让GlobalExceptionHandler处理，以便返回正确的HTTP状态码
            if (throwable instanceof ServiceException) {
                ServiceException serviceException = (ServiceException)throwable;
                Integer code = serviceException.getCode();
                if (code != null && isAuthenticationException(code)) {
                    throw throwable;
                }
            }

            // 根据异常类型封装成AjaxResult返回，不再抛出异常
            return handleException(throwable);
        }
    }

    /**
     * 处理异常并封装成AjaxResult返回
     *
     * @param throwable 异常信息
     * @return AjaxResult
     */
    private AjaxResult handleException(Throwable throwable) {
        // 业务异常
        if (throwable instanceof ServiceException) {
            ServiceException serviceException = (ServiceException)throwable;
            Integer code = serviceException.getCode();
            return code != null ? AjaxResult.error(code, serviceException.getMessage()) : AjaxResult.error(serviceException.getMessage());
        }

        // 运行时异常
        if (throwable instanceof RuntimeException) {
            return AjaxResult.error(throwable.getMessage());
        }

        // 参数校验异常
        if (throwable instanceof IllegalArgumentException) {
            return AjaxResult.error("参数校验失败: " + throwable.getMessage());
        }

        // 其他异常
        return AjaxResult.error("系统异常: " + throwable.getMessage());
    }

    /**
     * 记录异常日志
     *
     * @param joinPoint 切点信息
     * @param throwable 异常信息
     */
    private void logException(ProceedingJoinPoint joinPoint, Throwable throwable) {
        try {
            // 获取方法签名
            MethodSignature signature = (MethodSignature)joinPoint.getSignature();
            String className = signature.getDeclaringType().getSimpleName();
            String methodName = signature.getMethod().getName();

            // 获取方法参数
            Object[] args = joinPoint.getArgs();
            String argsJson = "无参数";
            if (args != null && args.length > 0) {
                try {
                    // 过滤掉不需要序列化的参数类型（如HttpServletRequest, HttpServletResponse等）
                    Object[] filteredArgs = Arrays.stream(args)
                        .filter(arg -> arg != null &&
                            !arg.getClass().getName().startsWith("jakarta.servlet") &&
                            !arg.getClass().getName().startsWith("javax.servlet") &&
                            !arg.getClass().getName().startsWith("org.springframework.web"))
                        .toArray();

                    if (filteredArgs.length > 0) {
                        argsJson = JSON.toJSONString(filteredArgs);
                    }
                } catch (Exception e) {
                    argsJson = "参数序列化失败: " + e.getMessage();
                }
            }

            // 获取请求信息
            String requestInfo = getRequestInfo();

            // 记录异常日志
            log.error("=== Controller层异常日志 ===\n" +
                "请求信息: {}\n" +
                "异常方法: {}.{}\n" +
                "入参数据: {}\n" +
                "异常类型: {}\n" +
                "异常消息: {}\n" +
                "异常堆栈: ",
                requestInfo,
                className,
                methodName,
                argsJson,
                throwable.getClass().getSimpleName(),
                throwable.getMessage(),
                throwable);

        } catch (Exception e) {
            // 记录日志时出现异常，避免影响原有业务流程
            log.error("记录Controller异常日志时发生错误", e);
        }
    }

    /**
     * 获取HTTP请求信息
     *
     * @return 请求信息字符串
     */
    private String getRequestInfo() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return String.format("%s %s, IP: %s, UserAgent: %s",
                    request.getMethod(),
                    request.getRequestURI(),
                    getClientIpAddress(request),
                    request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            return "获取请求信息失败: " + e.getMessage();
        }
        return "无请求信息";
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR", "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"};

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 多个IP的情况，取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                return ip.trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 判断是否为认证相关异常
     *
     * @param code 异常代码
     * @return 是否为认证异常
     */
    private boolean isAuthenticationException(Integer code) {
        return code.equals(ServiceExceptionEnum.TOKEN_INVALID.getCode()) ||
            code.equals(ServiceExceptionEnum.TOKEN_EXPIRED.getCode()) ||
            code.equals(ServiceExceptionEnum.LOGIN_REQUIRED.getCode());
    }
}