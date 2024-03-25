package io.github.lunasaw.voglander.common.exception;

import com.luna.common.exception.BaseException;

import lombok.Getter;

/**
 * 业务异常
 * 
 * @author luna
 */
@Getter
public final class ServiceException extends BaseException {

    public static final ServiceException TOO_FREQUENT_VISITS        = new ServiceException(700001, "访问过于频繁，请稍候再试");
    public static final ServiceException SERVER_THROTTLING_ABNORMAL = new ServiceException(700002, "服务器限流异常，请稍候再试");

    private static final long            serialVersionUID           = 1L;

    /**
     * 错误明细，内部调试错误
     **/
    private String                       detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException() {}

    public ServiceException(ServiceExceptionEnum exceptionEnum) {
        super(exceptionEnum.getCode(), exceptionEnum.getMessage());
    }

    public ServiceException(ServiceExceptionEnum exceptionEnumCode, String... extendMessage) {
        this(exceptionEnumCode.getCode(),
            String.format(exceptionEnumCode.getMessage(), extendMessage));
    }

    public ServiceException(Integer code, String message) {
        super(code, message);
    }

    public ServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }
}