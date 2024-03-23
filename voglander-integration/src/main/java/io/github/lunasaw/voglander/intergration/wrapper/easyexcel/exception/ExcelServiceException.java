package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.exception;

import com.luna.common.exception.BaseException;

import lombok.Getter;

/**
 * 自定义业务异常
 * 
 * @author luna
 */
@Getter
public final class ExcelServiceException extends BaseException {

    public static final ExcelServiceException EXCEL_RECORDS_ISNULL     = new ExcelServiceException(7000003, "Excel的文件内容不能为空");
    public static final ExcelServiceException EXCEL_FILE_PATH_ISNULL   = new ExcelServiceException(7000002, "暂存Excel的文件路径为空");
    public static final ExcelServiceException TABLE_HEAD_ISNULL        = new ExcelServiceException(7000004, "表头不能为空");
    public static final ExcelServiceException EXCEL_READ_EXCEPTION     = new ExcelServiceException(7000008, "Excel读文件发生异常");

    public static final ExcelServiceException HEAD_MATCH_EXCEPTION     = new ExcelServiceException(7000010, "表头的匹配结果为空");
    public static final ExcelServiceException GEN_EXCEL_TEMP_EXCEPTION = new ExcelServiceException(7000011, "生成Excel模板发生异常");
    public static final ExcelServiceException IMPORT_EXCEL_EXCEPTION   = new ExcelServiceException(7000012, "导入Excel发生异常");

    private static final long                 serialVersionUID         = 1L;

    /**
     * 错误明细，内部调试错误
     **/
    private String                            detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ExcelServiceException() {}

    public ExcelServiceException(ExcelExceptionEnums exceptionEnum) {
        super(exceptionEnum.getCode(), exceptionEnum.getMessage());
    }

    public ExcelServiceException(ExcelExceptionEnums exceptionEnumCode, String... extendMessage) {
        this(exceptionEnumCode.getCode(),
            String.format(exceptionEnumCode.getMessage(), extendMessage));
    }

    public ExcelServiceException(Integer code, String message) {
        super(code, message);
    }

    public ExcelServiceException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }
}