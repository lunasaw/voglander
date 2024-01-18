package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;
import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author luna
 */
@Data
@Api(value = "基础excelDTO", tags = "基础excelDTO")
public class BaseExcelDTO<T> implements Serializable {
    @ApiModelProperty(value = "WriterExcel基础类")
    private ExcelWriterDTO    baseWriterExcelDto;

    @ApiModelProperty(value = "需要写入excel的数据")
    private List<T>           records;

    @ApiModelProperty(value = "sheet的基础属性")
    private BaseExcelSheetDTO baseExcelSheetDto;

}
