package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.impl;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.google.common.collect.Lists;
import com.luna.common.check.AssertUtil;
import com.luna.common.constant.Constant;
import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import com.luna.common.exception.BaseException;

import io.github.lunasaw.voglander.client.domain.excel.ExcelReadBean;
import io.github.lunasaw.voglander.client.domain.excel.ExcelWriteBean;
import io.github.lunasaw.voglander.client.domain.excel.dto.*;
import io.github.lunasaw.voglander.client.service.excel.ExcelInnerService;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.dto.ExcelInnerReadBean;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.exception.ExcelServiceException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 */
@Slf4j
@Service
public class EasyExcelInnerServiceImpl implements ExcelInnerService {

    private static final String SHEET_NAME = "模板";

    @Override
    public <T> ResultDTO<Void> doWrite(ExcelWriteBean<T> writeBean) {
        AssertUtil.notNull(writeBean, BaseException.PARAMETER_ERROR);
        AssertUtil.notNull(writeBean.getTClass(), BaseException.PARAMETER_ERROR);
        BaseExcelDTO baseExcelDto = writeBean.getBaseExcelDto();
        AssertUtil.notNull(baseExcelDto, BaseException.PARAMETER_ERROR);

        Integer sheetNo = Optional.ofNullable(baseExcelDto.getBaseExcelSheetDto()).map(BaseExcelSheetDTO::getSheetNo).orElse(0);
        String sheetName = Optional.ofNullable(baseExcelDto.getBaseExcelSheetDto()).map(BaseExcelSheetDTO::getSheetName).orElse(SHEET_NAME);

        ExcelWriter excelWriter;
        ExcelBeanDTO baseWriterExcelDto = baseExcelDto.getExcelBeanDTO();
        if (baseWriterExcelDto.getExcelObj() instanceof ExcelWriter) {
            excelWriter = (ExcelWriter)baseWriterExcelDto.getExcelObj();
        } else {
            excelWriter = EasyExcel.write(writeBean.getTempPath(), writeBean.getTClass())
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .password(writeBean.getPassword())
                .build();
            baseWriterExcelDto.setExcelObj(excelWriter);
        }

        WriteSheet writeSheet;
        if (baseWriterExcelDto.getSheetObj() instanceof WriteSheet) {
            writeSheet = (WriteSheet)baseWriterExcelDto.getSheetObj();
        } else {
            writeSheet = EasyExcel.writerSheet(sheetNo, sheetName).build();
            List<List<String>> headList = Optional.ofNullable(writeBean.getHeadList()).orElse(new ArrayList<>());
            if (CollectionUtils.isNotEmpty(headList)) {
                writeSheet.setHead(headList);
            }
            Set<String> columnFiledNames = writeBean.getIncludeColumnFiledNames();
            if (CollectionUtils.isNotEmpty(columnFiledNames)) {
                writeSheet.setIncludeColumnFieldNames(columnFiledNames);
            }
            baseWriterExcelDto.setSheetObj(writeSheet);
        }

        try {
            String tableHead = writeBean.getTitleForTableHead();
            excelWriter.write(Lists.newArrayList(tableHead), writeSheet);
            List<T> datalist = Optional.ofNullable(writeBean.getDatalist()).orElse(new ArrayList<>());
            List<List<T>> partition = Lists.partition(datalist, Constant.NUMBER_HUNDERD);
            for (List<T> list : partition) {
                excelWriter.write(list, writeSheet);
            }
            return ResultDTOUtils.success();
        } catch (Exception e) {
            throw ExcelServiceException.IMPORT_EXCEL_EXCEPTION;
        }
    }

    @Override
    public <T> ResultDTO<Void> doWriteFinish(ExcelWriteBean<T> writeBean) {
        AssertUtil.notNull(writeBean, ServiceException.PARAMETER_ERROR);
        BaseExcelDTO baseExcelDto = writeBean.getBaseExcelDto();
        AssertUtil.notNull(baseExcelDto, ServiceException.PARAMETER_ERROR);

        ExcelBeanDTO baseWriterExcelDto = baseExcelDto.getExcelBeanDTO();
        if (baseWriterExcelDto.getExcelObj() instanceof ExcelWriter) {
            ExcelWriter excelWriter = (ExcelWriter)baseWriterExcelDto.getExcelObj();
            excelWriter.finish();
        }
        return ResultDTOUtils.success();
    }

    @Override
    public <T> ResultDTO<ExcelReadResultDTO<T>> readExcel(ExcelReadBean<T> excelReadBean) {
        AssertUtil.notNull(excelReadBean, ExcelServiceException.EXCEL_RECORDS_ISNULL);
        AssertUtil.notNull(excelReadBean.getTClass(), ExcelServiceException.EXCEL_READ_EXCEPTION);
        AssertUtil.isTrue(excelReadBean.getFilePath() != null || excelReadBean.getInputStream() != null,
            ExcelServiceException.EXCEL_FILE_PATH_ISNULL);

        ExcelReadResultDTO<T> excelReadResultDTO = Optional.ofNullable(excelReadBean.getExcelReadResultDTO()).orElse(new ExcelReadResultDTO<>());
        BaseExcelDTO baseExcelDTO = Optional.ofNullable(excelReadBean.getBaseExcelDTO()).orElse(new BaseExcelDTO());
        ExcelBeanDTO excelBeanDTO = Optional.ofNullable(baseExcelDTO.getExcelBeanDTO()).orElse(new ExcelBeanDTO());

        ExcelDataListener<T> excelDataListener = new ExcelDataListener<>(excelReadResultDTO);
        // 如果是监听模型，需要保存数据
        if (excelReadBean instanceof ExcelInnerReadBean) {
            ExcelInnerReadBean<T> excelInnerReadReq = (ExcelInnerReadBean<T>)excelReadBean;
            if (excelInnerReadReq.getSaveDataFunction() != null) {
                excelDataListener = new ExcelDataListener<>(excelInnerReadReq.getSaveDataFunction(), excelReadResultDTO);
            }
        }

        ExcelReader excelReader;
        if (excelBeanDTO.getExcelObj() instanceof ExcelReader) {
            excelReader = (ExcelReader)excelBeanDTO.getExcelObj();
        } else {
            if (StringUtils.isNotEmpty(excelReadBean.getFilePath())) {
                excelReader = EasyExcel.read(excelReadBean.getFilePath(), excelReadBean.getTClass(), excelDataListener)
                    .headRowNumber(excelReadBean.getHeadRowNumber())
                    .build();
            } else if (excelReadBean.getInputStream() != null) {
                excelReader =
                    EasyExcel.read(excelReadBean.getInputStream(), excelReadBean.getTClass(), excelDataListener)
                        .headRowNumber(excelReadBean.getHeadRowNumber())
                        .build();
            } else {
                throw ExcelServiceException.EXCEL_FILE_PATH_ISNULL;
            }
        }
        excelBeanDTO.setExcelObj(excelReader);
        excelReadBean.setExcelReadResultDTO(excelReadResultDTO);
        excelReadBean.setBaseExcelDTO(baseExcelDTO);

        List<ReadSheet> sheets = excelReader.excelExecutor().sheetList();
        for (ReadSheet sheet : sheets) {
            excelBeanDTO.setSheetObj(sheet);
            excelReader.read(sheet);
        }

        return ResultDTOUtils.success(excelReadResultDTO);
    }

    @Override
    public ResultDTO<String> geneTempFile(GeneTempDTO geneTempDto) {
        AssertUtil.notNull(geneTempDto, ServiceException.PARAMETER_ERROR);
        AssertUtil.notEmpty(geneTempDto.getReadSetMap(), ExcelServiceException.EXCEL_FILE_PATH_ISNULL);

        List<List<String>> headList = new Vector<>();
        Map<String, String> readSetMap = geneTempDto.getReadSetMap();

        if (MapUtils.isEmpty(readSetMap)) {
            throw ExcelServiceException.TABLE_HEAD_ISNULL;
        }

        readSetMap.forEach((colName, value) -> {
            List<String> head = new ArrayList<>();
            head.add(Optional.ofNullable(geneTempDto.getExplainStr()).orElse(StringUtils.EMPTY));
            head.add(Optional.ofNullable(geneTempDto.getExampleMap().get(colName)).orElse(StringUtils.EMPTY));
            head.add(geneTempDto.getReadSetMap().get(colName));
            headList.add(head);
        });

        ExcelWriteBean<Map<String, String>> excelWriteBean = new ExcelWriteBean<>();
        BaseExcelDTO baseExcelDTO = new BaseExcelDTO();
        BaseExcelSheetDTO baseExcelSheetDTO = new BaseExcelSheetDTO(0);
        baseExcelSheetDTO.setSheetName(SHEET_NAME);
        baseExcelDTO.setBaseExcelSheetDto(baseExcelSheetDTO);
        excelWriteBean.setBaseExcelDto(baseExcelDTO);
        excelWriteBean.setTempPath(geneTempDto.getFilePath());
        excelWriteBean.setHeadList(headList);
        excelWriteBean.setDatalist(Lists.newArrayList(geneTempDto.getExampleMap()));
        doWrite(excelWriteBean);
        doWriteFinish(excelWriteBean);

        return ResultDTOUtils.success(geneTempDto.getFilePath());
    }
}
