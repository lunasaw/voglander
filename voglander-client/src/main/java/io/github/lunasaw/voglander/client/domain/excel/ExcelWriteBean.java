package io.github.lunasaw.voglander.client.domain.excel;

import io.github.lunasaw.voglander.client.domain.excel.dto.BaseExcelDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

/**
 * @author luna
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
     * 加密密码
     */
    private String                password;

    /**
     * 表头的题头
     */
    private String                titleForTableHead;

    /**
     * 表头数据 自定义表头
     */
    private List<List<String>>    headList                = new ArrayList<>();

    /**
     * 数据
     */
    private List<T>               datalist                = new ArrayList<>();

    /**
     * 格式类
     */
    private Class<T>              tClass                  = (Class<T>)Object.class;

    /**
     * 宽度
     */
    private Map<Integer, Integer> getColumnWidthMap       = new HashMap<>();

    /**
     * 需要的列 为空表示所有列
     */
    private Set<String>           includeColumnFiledNames = new HashSet<>();
}
