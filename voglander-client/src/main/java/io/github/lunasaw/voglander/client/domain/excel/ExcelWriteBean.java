package io.github.lunasaw.voglander.client.domain.excel;

import java.io.Serializable;
import java.util.*;

import io.github.lunasaw.voglander.client.domain.excel.dto.BaseExcelDTO;
import lombok.Data;

/**
 * @author luna
 * excel的read的辅助bean
 * 读取bean 都用字符串读取 忽略表头，或者明确excel格式一致的时候 可以指定读取行，从数据行读取，不然会出现格式化问题
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
    private Class<T>              tClass;

    /**
     * 宽度
     */
    private Map<Integer, Integer> getColumnWidthMap       = new HashMap<>();

    /**
     * 需要的列 为空表示所有列
     */
    private Set<String>           includeColumnFiledNames = new HashSet<>();
}
