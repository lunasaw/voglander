package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * @author weidian
 * excel的write的辅助bean
 */
@Data
public class ExcelWriteBean<T> implements Serializable {
    /**
     * 基础excelDTO
     */
    private BaseExcelDTO          baseExcelDto;

    /**
     * 写入路径
     */
    private String                tempPath;
    /**
     * 表头的题头
     */
    private String                titleForTableHead;
    /**
     * 表头数据 自定义表头
     */
    private List<List<String>>    headList          = new ArrayList<>();

    /**
     * 数据
     */
    private List<T>               datalist          = new ArrayList<>();

    /**
     * 格式类
     */
    private Class<T>              tClass            = (Class<T>)Object.class;

    /**
     * 宽度
     */
    private Map<Integer, Integer> getColumnWidthMap = new HashMap<>();

}
