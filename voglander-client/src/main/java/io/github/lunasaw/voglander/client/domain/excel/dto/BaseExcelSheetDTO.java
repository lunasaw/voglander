package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;

import io.github.lunasaw.voglander.common.constant.ExcelApiConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author luna
 */
@Data
@Api(value = "基础excelSheetDTO", tags = "基础excelSheetDTO")
public class BaseExcelSheetDTO implements Serializable {

    @ApiModelProperty(value = "sheet序号")
    private Integer sheetNo;

    @ApiModelProperty(value = "sheet名称")
    private String  sheetName;

    @ApiModelProperty(value = "从第几行开始写数据")
    private Integer startColumn;

    public BaseExcelSheetDTO(Integer sheetNo) {
        this.sheetNo = sheetNo;
        this.sheetName = ExcelApiConstants.ExcelSheetDefault.prefixForSheetName + sheetNo;
    }
}
