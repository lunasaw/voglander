package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.dto;

import io.github.lunasaw.voglander.client.domain.excel.ExcelReadBean;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call.SaveDataFunction;
import lombok.Data;

/**
 * @author luna
 * @date 2024/1/18
 */
@Data
public class ExcelInnerReadBean<T> extends ExcelReadBean {

    private SaveDataFunction<T> saveDataFunction;

}
