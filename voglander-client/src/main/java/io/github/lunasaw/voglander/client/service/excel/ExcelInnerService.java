package io.github.lunasaw.voglander.client.service.excel;

import io.github.lunasaw.voglander.client.domain.excel.dto.GeneTempDTO;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelReadReq;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelWriterReq;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadDTO;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelWriteBean;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelWriterDTO;

/**
 * @author luna
 */
public interface ExcelInnerService {
    /**
     * 往excel里面写数据
     * 
     * @param writeBean
     */
    void doWrite(ExcelWriteBean writeBean);

    /**
     * 获取WriterExcel
     * 
     * @param excelWriterReq
     * @return
     */
    public ExcelWriterDTO getBaseWiterExcelDto(ExcelWriterReq excelWriterReq);

    /**
     * 刷新WriterExcel
     * 
     * @param baseWriterExcelDto (WriterExcel)
     * @return
     */
    public void flushWiterExcel(ExcelWriterDTO baseWriterExcelDto);

    /**
     * 获取WriterExcel处理实现类的辅助bean(比如自定义的一些东西)
     * 
     * @return
     */
    public Object getExcelWriteDealBean();

    /**
     * 解析excel里面的数据
     * 
     * @param excelReadReq
     */
    public ExcelReadDTO readExcel(ExcelReadReq excelReadReq);

    /**
     * 生成模板文件
     * 
     * @param geneTempDto (GeneTempDTO)
     * @return
     */
    public String geneTempFile(GeneTempDTO geneTempDto);

}
