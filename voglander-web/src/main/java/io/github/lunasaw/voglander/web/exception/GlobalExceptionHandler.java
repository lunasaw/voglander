package io.github.lunasaw.voglander.web.exception;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;
import java.util.Objects;

/**
 * 全局异常处理器
 *
 * @author luna
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 权限校验异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限校验失败'{}'", requestURI, e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN.value(), "没有权限，请联系管理员授权");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                          HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<AjaxResult> handleServiceException(ServiceException e, HttpServletRequest request) {
        Integer code = e.getCode();
        HttpStatus domainStatus = getDomainHttpStatus(code);
        if (domainStatus == null) {
            log.error(e.getMessage(), e);
        } else {
            log.warn("Domain request rejected: uri={}, code={}", request.getRequestURI(), code);
        }
        AjaxResult result = Objects.nonNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());

        // 处理认证相关异常，返回401状态码
        if (Objects.nonNull(code)) {
            if (code.equals(ServiceExceptionEnum.TOKEN_INVALID.getCode()) ||
                code.equals(ServiceExceptionEnum.TOKEN_EXPIRED.getCode()) ||
                code.equals(ServiceExceptionEnum.LOGIN_REQUIRED.getCode())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
            }
            if (domainStatus != null) {
                return ResponseEntity.status(domainStatus).body(result);
            }
        }

        return ResponseEntity.ok(result);
    }

    /** Missing credentials are authentication failures; other required headers are bad requests. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<AjaxResult> handleMissingRequestHeaderException(MissingRequestHeaderException e,
        HttpServletRequest request) {
        ServiceExceptionEnum error = "Authorization".equalsIgnoreCase(e.getHeaderName())
            ? ServiceExceptionEnum.LOGIN_REQUIRED : ServiceExceptionEnum.PARAM_ERROR;
        return handleServiceException(new ServiceException(error), request);
    }

    private static HttpStatus getDomainHttpStatus(Integer code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case 600001, 710000, 710002, 710003, 710010, 710011, 710012, 720004, 720005, 720007, 720010 -> HttpStatus.BAD_REQUEST;
            case 710001 -> HttpStatus.PAYLOAD_TOO_LARGE;
            case 710004, 710014, 720000, 720001, 720002 -> HttpStatus.NOT_FOUND;
            case 600007, 710005, 710015, 720003, 720008, 720011 -> HttpStatus.CONFLICT;
            case 710009 -> HttpStatus.GONE;
            case 710006, 710007, 710008, 710013, 720009, 720012 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 710016 -> HttpStatus.GATEWAY_TIMEOUT;
            case 710017 -> HttpStatus.BAD_GATEWAY;
            case 710018, 720006 -> HttpStatus.FORBIDDEN;
            default -> null;
        };
    }

    /**
     * 请求路径中缺少必需的路径变量
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), e.getValue()));
    }

    /**
     * 网络连接异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        // 客户端主动断开 SSE/长连接（关页面/导航/网络抖动）是常态，降级为 WARN，不写回 body
        if (isClientAbort(e)) {
            log.warn("客户端连接已断开，忽略写回 - 请求地址: {}, 原因: {}", requestURI, rootMessage(e));
            return null;
        }

        // 特殊处理网络连接异常
        if (e.getCause() instanceof java.net.SocketException) {
            if (requestURI.contains("/zlm/api/")) {
                log.error("ZLM服务连接异常 - 请求地址: {}, 错误: {}", requestURI, e.getMessage());
                return AjaxResult.error("ZLM媒体服务器连接失败，请检查服务状态");
            }
            log.error("网络连接异常 - 请求地址: {}, 错误: {}", requestURI, e.getMessage());
            return AjaxResult.error("网络连接异常，请稍后重试");
        }

        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();

        // 客户端主动断开 SSE/长连接是常态（典型：SseEmitter heartbeat 向已断连接 send 抛 Broken pipe，
        // 经 Tomcat 异步 onError 冒泡到此）。降级为 WARN 并返回 null——响应已是 text/event-stream
        // 且连接已断，不能再写 AjaxResult（否则二次抛 HttpMessageNotWritableException）。
        if (isClientAbort(e)) {
            log.warn("客户端连接已断开，忽略写回 - 请求地址: {}, 原因: {}", requestURI, rootMessage(e));
            return null;
        }

        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 判定异常链是否为「客户端主动断开连接」——这类异常不可控、不可恢复、非系统缺陷，
     * 不应记 ERROR。覆盖：Tomcat ClientAbortException、Spring AsyncRequestNotUsableException、
     * 以及底层 Broken pipe / Connection reset 等 socket 写失败。
     *
     * @param e 异常（含 cause 链）
     * @return true=客户端断连导致的写失败
     */
    private static boolean isClientAbort(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String className = t.getClass().getName();
            if (className.contains("ClientAbortException")
                || className.contains("AsyncRequestNotUsableException")) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Broken pipe")
                || msg.contains("Connection reset")
                || msg.contains("connection was aborted")
                || msg.contains("Connection reset by peer"))) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /**
     * 取异常链最底层的可读消息（用于 WARN 日志）。
     */
    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e) {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(message);
    }
}
