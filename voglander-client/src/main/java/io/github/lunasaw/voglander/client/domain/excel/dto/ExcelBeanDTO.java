package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author luna
 */
@Data
@Api(value = "ExcelWriter基础类", tags = "ExcelWriter基础类")
public class ExcelBeanDTO implements Serializable {

    @ApiModelProperty(value = "excel的writer类暂存")
    private Object excelObj;

    @ApiModelProperty(value = "excel的当前sheet暂存")
    private Object sheetObj;
}
