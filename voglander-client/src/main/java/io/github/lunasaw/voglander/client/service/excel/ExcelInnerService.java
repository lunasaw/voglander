package io.github.lunasaw.voglander.client.service.excel;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadResultDTO;
import io.github.lunasaw.voglander.client.domain.excel.ExcelWriteBean;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelBeanDTO;
import io.github.lunasaw.voglander.client.domain.excel.dto.GeneTempDTO;
import io.github.lunasaw.voglander.client.domain.excel.ExcelReadBean;

/**
 * @author luna
 */
public interface ExcelInnerService {
    /**
     * 往excel里面写数据
     * 
     * @param writeBean
     */
    <T> ResultDTO<Void> doWrite(ExcelWriteBean<T> writeBean);

    /**
     * 执行完成写入文件
     * 
     * @param writeBean
     */
    <T> ResultDTO<Void> doWriteFinish(ExcelWriteBean<T> writeBean);

    /**
     * 刷新WriterExcel
     * 
     * @param baseWriterExcelDto (WriterExcel)
     * @return
     */
    ResultDTO<Void> flushWiterExcel(ExcelBeanDTO baseWriterExcelDto);

    /**
     * 解析excel里面的数据
     * 
     * @param excelReadBean
     */
    <T> ResultDTO<ExcelReadResultDTO<T>> readExcel(ExcelReadBean<T> excelReadBean);

    /**
     * 生成模板文件
     * 
     * @param geneTempDto (GeneTempDTO)
     * @return
     */
    ResultDTO<String> geneTempFile(GeneTempDTO geneTempDto);

}
