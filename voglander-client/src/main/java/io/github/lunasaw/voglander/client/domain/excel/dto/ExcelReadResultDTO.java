package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.Api;
import lombok.Data;

/**
 * @author luna
 */
@Data
@Api(value = "ExcelRead基础类", tags = "ExcelRead基础类")
public class ExcelReadResultDTO<T> implements Serializable {
    /**
     * key是需要 value是名称
     */
    private Map<Integer/*index*/, String/*data*/>       headMap        = new HashMap<>();

    /**
     * key是字段的名称 value是sheet对应的值
     */
    private List<Map<Integer/*index*/, String/*data*/>> readResultMap  = new ArrayList<>();

    /**
     * 序列化后的数据
     */
    private List<T>                                     readResultList = new ArrayList<>();

}
