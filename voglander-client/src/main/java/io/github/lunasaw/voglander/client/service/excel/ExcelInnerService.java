package io.github.lunasaw.voglander.client.service.excel;

import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadDTO;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelWriteBean;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelWriterDTO;
import io.github.lunasaw.voglander.client.domain.excel.dto.GeneTempDTO;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelReadReq;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelWriterReq;

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
     * 获取WriterExcel
     * 
     * @param excelWriterReq
     * @return
     */
    ResultDTO<ExcelWriterDTO> getBaseWiterExcelDto(ExcelWriterReq excelWriterReq);

    /**
     * 刷新WriterExcel
     * 
     * @param baseWriterExcelDto (WriterExcel)
     * @return
     */
    ResultDTO<Void> flushWiterExcel(ExcelWriterDTO baseWriterExcelDto);

    /**
     * 获取WriterExcel处理实现类的辅助bean(比如自定义的一些东西)
     * 
     * @return
     */
    ResultDTO<Object> getExcelWriteDealBean();

    /**
     * 解析excel里面的数据
     * 
     * @param excelReadReq
     */
    ResultDTO<ExcelReadDTO> readExcel(ExcelReadReq excelReadReq);

    /**
     * 生成模板文件
     * 
     * @param geneTempDto (GeneTempDTO)
     * @return
     */
    ResultDTO<String> geneTempFile(GeneTempDTO geneTempDto);

}
