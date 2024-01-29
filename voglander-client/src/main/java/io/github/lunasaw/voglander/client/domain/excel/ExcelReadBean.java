package io.github.lunasaw.voglander.client.domain.excel;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import io.github.lunasaw.voglander.client.domain.excel.dto.BaseExcelDTO;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadResultDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author weidian
 */
@Data
@Api(value = "ExcelReadReq类", tags = "ExcelReadReq类")
public class ExcelReadBean<T> implements Serializable {
    /**
     * 基础excelDTO
     */
    private BaseExcelDTO          baseExcelDTO;

    /**
     * Excel读取结果
     */
    private ExcelReadResultDTO<T> excelReadResultDTO;

    /**
     * 格式类
     */
    private Class<T>              tClass        = (Class<T>)Object.class;

    @ApiModelProperty(value = "excel的当前sheet暂存文件路径")
    private String                filePath;
    @ApiModelProperty(value = "excel的当前sheet暂存文件的输入流")
    private InputStream           inputStream;

    @ApiModelProperty(value = "说明行")
    private Integer               explainColumn = 0;

    @ApiModelProperty(value = "表头行")
    private Integer               headColumn    = 1;

    @ApiModelProperty(value = "示列行")
    private Integer               exampleColumn = 2;


    private Integer               headRowNumber = 0;

}
