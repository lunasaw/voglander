package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.google.common.collect.Lists;
import com.luna.common.constant.Constant;

import io.github.lunasaw.voglander.client.domain.excel.dto.*;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelReadReq;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelWriterReq;
import io.github.lunasaw.voglander.client.service.excel.ExcelInnerService;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call.HeaderMergeStrategy;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.dto.ExcelInnerReadReq;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.exception.ExcelExceptionEnums;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.strategy.MatchColumnWidthStyleStrategy;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

/**
 * @author weidian
 */
@Api(value = "EasyExcel的实现", tags = "EasyExcel的实现")
@Slf4j
@Service
public class EasyExcelInnerServiceImpl implements ExcelInnerService {

    @Override
    public <T> void doWrite(ExcelWriteBean<T> writeBean) {
        BaseExcelDTO baseExcelDto = writeBean.getBaseExcelDto();
        Assert.notNull(baseExcelDto, "baseExcelDto can not be null");

        Integer sheetNo = baseExcelDto.getBaseExcelSheetDto().getSheetNo();
        String sheetName = baseExcelDto.getBaseExcelSheetDto().getSheetName();

        ExcelWriter excelWriter;
        ExcelWriterDTO baseWriterExcelDto = baseExcelDto.getExcelWriterDTO();
        if (baseWriterExcelDto.getExcelWriter() instanceof ExcelWriter) {
            excelWriter = (ExcelWriter)baseWriterExcelDto.getExcelWriter();
        } else {
            excelWriter = EasyExcel.write(writeBean.getTempPath(), writeBean.getTClass())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .registerWriteHandler(new HeaderMergeStrategy())
                .build();
            baseWriterExcelDto.setExcelWriter(excelWriter);
        }

        WriteSheet writeSheet;
        if (baseWriterExcelDto.getWriteSheet() instanceof WriteSheet) {
            writeSheet = (WriteSheet)baseWriterExcelDto.getWriteSheet();
        } else {
            writeSheet = EasyExcel.writerSheet(sheetNo, sheetName).build();
            if (!CollectionUtils.isEmpty(writeBean.getHeadList())) {
                writeSheet.setHead(writeBean.getHeadList());
            }
            baseWriterExcelDto.setWriteSheet(writeSheet);
        }

        try {
            excelWriter.write(Lists.newArrayList(writeBean.getTitleForTableHead()), writeSheet);
            List<List<T>> partition = Lists.partition(writeBean.getDatalist(), Constant.FIVE_THOUSAND);
            for (List<T> list : partition) {
                excelWriter.write(list, writeSheet);
            }
        } catch (Exception e) {
            log.error("doWrite::error", e);
            throw new ServiceException(ExcelExceptionEnums.IMPORT_EXCEL_EXCEPTION.getDesc());
        }
    }

    @Override
    public <T> void doWriteFinish(ExcelWriteBean<T> writeBean) {
        Assert.notNull(writeBean, "writeBean can not be null");
        BaseExcelDTO baseExcelDto = writeBean.getBaseExcelDto();
        Assert.notNull(baseExcelDto, "baseExcelDto can not be null");
        ExcelWriterDTO baseWriterExcelDto = baseExcelDto.getExcelWriterDTO();
        if (baseWriterExcelDto.getExcelWriter() instanceof ExcelWriter) {
            ExcelWriter excelWriter = (ExcelWriter)baseWriterExcelDto.getExcelWriter();
            excelWriter.finish();
        }
    }

    @Override
    public ExcelWriterDTO getBaseWiterExcelDto(ExcelWriterReq excelWriterReq) {
        ExcelWriterDTO baseWriterExcelDto = new ExcelWriterDTO();
        MatchColumnWidthStyleStrategy matchColumnWidthStyleStrategy = new MatchColumnWidthStyleStrategy();
        if (MapUtils.isNotEmpty(excelWriterReq.getColumnWidthMap())) {
            matchColumnWidthStyleStrategy.setColumnWidthMap(excelWriterReq.getColumnWidthMap());
        }

        ExcelWriter excelWriter = null;
        if (StringUtils.isNotEmpty(excelWriterReq.getFilePath())) {
            excelWriter = EasyExcelFactory.write(excelWriterReq.getFilePath()).registerWriteHandler(matchColumnWidthStyleStrategy).build();
        } else if (excelWriterReq.getOutputStream() != null) {
            excelWriter = EasyExcelFactory.write(excelWriterReq.getOutputStream()).registerWriteHandler(matchColumnWidthStyleStrategy).build();
        } else {
            throw new ServiceException(ExcelExceptionEnums.EXCEL_FILE_PATH_ISNULL.getDesc());
        }
        baseWriterExcelDto.setExcelWriter(excelWriter);
        return baseWriterExcelDto;
    }

    @Override
    public void flushWiterExcel(ExcelWriterDTO baseWriterExcelDto) {
        if (baseWriterExcelDto.getExcelWriter() != null && baseWriterExcelDto.getExcelWriter() instanceof ExcelWriter) {
            ExcelWriter excelWriter = (ExcelWriter)baseWriterExcelDto.getExcelWriter();
            excelWriter.finish();
        }
    }

    @Override
    public Object getExcelWriteDealBean() {
        return null;
    }

    @Override
    public ExcelReadDTO readExcel(ExcelReadReq excelReadReq) {
        ExcelReadDTO excelReadDto = new ExcelReadDTO();
        ExcelReader excelReader;

        ExcelDataListener<Integer, String> excelDataListener = new ExcelDataListener<>(excelReadDto);
        if (excelReadReq instanceof ExcelInnerReadReq) {
            ExcelInnerReadReq excelInnerReadReq = (ExcelInnerReadReq)excelReadReq;
            if (excelInnerReadReq.getSaveDataFunction() != null) {
                excelDataListener = new ExcelDataListener<>(excelInnerReadReq.getSaveDataFunction(), excelReadDto);
            }
        }

        if (StringUtils.isNotEmpty(excelReadReq.getFilePath())) {
            excelReader = EasyExcel.read(excelReadReq.getFilePath(), excelDataListener).headRowNumber(excelReadReq.getHeadRowNumber()).build();
        } else if (excelReadReq.getInputStream() != null) {
            excelReader = EasyExcel.read(excelReadReq.getInputStream(), excelDataListener).headRowNumber(excelReadReq.getHeadRowNumber()).build();
        } else {
            throw new ServiceException(ExcelExceptionEnums.EXCEL_FILE_PATH_ISNULL.getDesc());
        }

        List<ReadSheet> sheets = excelReader.excelExecutor().sheetList();
        for (ReadSheet readSheet : sheets) {
            if (readSheet.getSheetNo() == 0) {
                // 只处理sheet no为0的sheet
                excelReader.read(readSheet);
            }
        }
        return excelReadDto;
    }

    @Override
    public String geneTempFile(GeneTempDTO geneTempDto) {
        if (StringUtils.isEmpty(geneTempDto.getFilePath())) {
            throw new ServiceException(ExcelExceptionEnums.EXCEL_FILE_PATH_ISNULL.getDesc());
        }
        List<List<String>> headList = new Vector<>();
        MatchColumnWidthStyleStrategy matchColumnWidthStyleStrategy = new MatchColumnWidthStyleStrategy();
        if (!CollectionUtils.isEmpty(geneTempDto.getReadSetMap())) {
            int i = 0;
            for (String colName : geneTempDto.getReadSetMap().keySet()) {
                List<String> head = new ArrayList<>();
                head.add(geneTempDto.getExplainStr() == null ? "" : geneTempDto.getExplainStr());
                String exampleStr = geneTempDto.getExampleMap().get(colName);
                if (StringUtils.isEmpty(exampleStr)) {
                    head.add("");
                } else {
                    head.add(exampleStr);
                }
                head.add(geneTempDto.getReadSetMap().get(colName));
                matchColumnWidthStyleStrategy.getColumnWidthMap().put(i, 100);
                headList.add(head);
                i++;
            }
        }
        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcelFactory.write(geneTempDto.getFilePath()).registerWriteHandler(matchColumnWidthStyleStrategy).build();
            WriteSheet writeSheet = EasyExcel.writerSheet(0, "模板").head(headList).build();
            excelWriter.write(new ArrayList<>(), writeSheet);
        } catch (Exception e) {
            throw new ServiceException(ExcelExceptionEnums.GEN_EXCEL_TEMP_EXCEPTION.getDesc());
        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
        return geneTempDto.getFilePath();
    }
}
