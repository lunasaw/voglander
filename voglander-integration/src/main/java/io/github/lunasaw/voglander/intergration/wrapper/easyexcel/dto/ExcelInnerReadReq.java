package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.dto;

import io.github.lunasaw.voglander.client.domain.excel.req.ExcelReadReq;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call.SaveDataFunction;
import lombok.Data;

/**
 * @author luna
 * @date 2024/1/18
 */
@Data
public class ExcelInnerReadReq extends ExcelReadReq {

    private SaveDataFunction<Integer, String> saveDataFunction;

}
