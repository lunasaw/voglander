package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * @author luna
 */
@Data

public class ExcelBeanDTO implements Serializable {


    private Object excelObj;


    private Object sheetObj;
}
