package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;

import io.github.lunasaw.voglander.common.constant.easyexcel.ExcelApiConstants;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author luna
 */
@Data

public class BaseExcelSheetDTO implements Serializable {


    private Integer sheetNo;


    private String  sheetName;


    private Integer startColumn;

    public BaseExcelSheetDTO(Integer sheetNo) {
        this.sheetNo = sheetNo;
        this.sheetName = ExcelApiConstants.ExcelSheetDefault.PREFIX_FOR_SHEET_NAME + sheetNo;
    }
}
