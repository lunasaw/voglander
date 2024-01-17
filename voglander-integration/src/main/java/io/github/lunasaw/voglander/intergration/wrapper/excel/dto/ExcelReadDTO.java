package io.github.lunasaw.voglander.intergration.wrapper.excel.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author luna
 */
@Data
@Api(value = "ExcelRead基础类", tags = "ExcelRead基础类")
public class ExcelReadDTO implements Serializable {
    /**
     * key是需要 value是名称
     */
    @ApiModelProperty(value = "匹配的表头")
    private Map<Integer, String>       headMap       = new HashMap<>();

    /**
     * key是字段的名称 value是sheet对应的值
     */
    @ApiModelProperty(value = "读的结果数据")
    private List<Map<Integer, String>> readResultMap = new ArrayList<>();

}
