package io.github.lunasaw.voglander.client.domain.excel;

import java.io.InputStream;
import java.io.Serializable;

import io.github.lunasaw.voglander.client.domain.excel.dto.BaseExcelDTO;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadResultDTO;
import lombok.Data;

/**
 * @author luna
 */
@Data

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
    private Class<T>              tClass;


    private String                filePath;

    private InputStream           inputStream;


    private Integer               explainColumn = 0;


    private Integer               headColumn    = 1;


    private Integer               exampleColumn = 2;

    private Integer               headRowNumber = 0;

}
