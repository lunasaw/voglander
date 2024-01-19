package io.github.lunasaw.voglander.client.domain.excel.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class BaseExcelConfig {
    @ApiModelProperty(value = "excel实现方式 ExcelImplType(0 EasyExcel)")
    private Integer excelType;

}
