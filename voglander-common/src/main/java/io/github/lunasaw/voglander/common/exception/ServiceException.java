package io.github.lunasaw.voglander.common.exception;

/**
 * 业务异常
 * 
 * @author luna
 */
public final class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer           code;

    /**
     * 错误提示
     */
    private String            message;

    /**
     * 错误明细，内部调试错误
     **/
    private String            detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException() {}

    public ServiceException(ServiceExceptionEnum exceptionEnum) {
        this.message = exceptionEnum.getMessage();
        this.code = exceptionEnum.getCode();
    }

    public ServiceException(String message) {
        this.message = message;
    }

    public ServiceException(ServiceExceptionEnum exceptionEnumCode, String... extendMessage) {
        this(exceptionEnumCode.getCode(),
            String.format(exceptionEnumCode.getMessage(), extendMessage));
    }

    public ServiceException(Integer code, String message) {
        this.message = message;
        this.code = code;
    }

    public ServiceException(String message, Integer code) {
        this.message = message;
        this.code = code;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public ServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ServiceException setMessage(String message) {
        this.message = message;
        return this;
    }

    public Integer getCode() {
        return code;
    }
}