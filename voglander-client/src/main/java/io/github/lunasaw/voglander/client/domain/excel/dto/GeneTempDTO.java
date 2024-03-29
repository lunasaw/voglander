package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;
import java.util.Map;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author luna
 */
@Data
@Api(value = "Excel导入模板", tags = "Excel导入模板")
public class GeneTempDTO implements Serializable {

    @ApiModelProperty(value = "读配置设置(ImportField注解的字段)")
    private Map<String, String> readSetMap;

    @ApiModelProperty(value = "示列数据")
    private Map<String, String> exampleMap;

    @ApiModelProperty(value = "导入说明")
    private String              explainStr;

    @ApiModelProperty(value = "用于存放导入结果的临时文件路径")
    private String              filePath;
}
