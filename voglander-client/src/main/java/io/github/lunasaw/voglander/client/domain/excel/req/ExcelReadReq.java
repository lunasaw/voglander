package io.github.lunasaw.voglander.client.domain.excel.req;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author weidian
 */
@Data
@Api(value = "ExcelReadReq类", tags = "ExcelReadReq类")
public class ExcelReadReq implements Serializable {
    @ApiModelProperty(value = "excel的当前sheet暂存文件路径")
    private String              filePath;
    @ApiModelProperty(value = "excel的当前sheet暂存文件的输入流")
    private InputStream         inputStream;

    @ApiModelProperty(value = "读配置设置")
    private Map<String, String> readSetMap;       // 引用ImportField注解的字段

    /*  @ApiModelProperty(value = "是否忽略表头的差异(true严格校验表头一致性,false对应上的注入,没对应的忽略)")
    private Boolean ignoreHeadDiff;*/

    @ApiModelProperty(value = "说明行")
    private Integer             explainColumn = 1;

    @ApiModelProperty(value = "示列行")
    private Integer             exampleColumn = 2;

    @ApiModelProperty(value = "表头行")
    private Integer             headColumn    = 3;

    private Integer             headRowNumber = 0;

}
