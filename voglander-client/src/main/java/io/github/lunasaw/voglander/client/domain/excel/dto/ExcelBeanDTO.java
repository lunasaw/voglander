package io.github.lunasaw.voglander.client.domain.excel.dto;

import java.io.Serializable;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author luna
 */
@Data

public class ExcelBeanDTO implements Serializable {


    private Object excelObj;


    private Object sheetObj;
}
