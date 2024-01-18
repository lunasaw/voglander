package io.github.lunasaw.voglander.intergration.doamin.excel.req;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@Api(value = "ExcelWriter基础类", tags = "ExcelWriter基础类")
public class ExcelWriterReq implements Serializable {
    @ApiModelProperty(value = "excel的当前sheet暂存文件路径")
    private String                filePath;
    @ApiModelProperty(value = "excel的当前sheet暂存文件输出流")
    private OutputStream          outputStream;

    // 宽度(只用于1个table)
    @ApiModelProperty(value = "excel的当前宽度对照关系")
    private Map<Integer, Integer> columnWidthMap = new HashMap<>();

    @ApiModelProperty(value = "excel实现特定的对象,比如EasyExcelBean,实现一些定制的需求")
    private Object                excelBean;
}