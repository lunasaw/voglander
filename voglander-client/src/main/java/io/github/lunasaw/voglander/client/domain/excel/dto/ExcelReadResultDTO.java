package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.swagger.annotations.Api;
import lombok.Data;

/**
 * @author luna
 */
@Data
@Api(value = "ExcelRead基础类", tags = "ExcelRead基础类")
public class ExcelReadResultDTO<T> implements Serializable {

    /**
     * 序列化后的数据
     */
    private List<T> readResultList = new ArrayList<>();

}
