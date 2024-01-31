package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.impl;

import java.util.*;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.google.common.collect.Lists;
import com.luna.common.constant.Constant;
import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;

import io.github.lunasaw.voglander.client.domain.excel.ExcelReadBean;
import io.github.lunasaw.voglander.client.domain.excel.ExcelWriteBean;
import io.github.lunasaw.voglander.client.domain.excel.dto.*;
import io.github.lunasaw.voglander.client.service.excel.ExcelInnerService;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.dto.ExcelInnerReadBean;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.exception.ExcelExceptionEnums;
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
    public <T> ResultDTO<Void> doWrite(ExcelWriteBean<T> writeBean) {
        BaseExcelDTO baseExcelDto = writeBean.getBaseExcelDto();
        Assert.notNull(baseExcelDto, "baseExcelDto can not be null");

        Integer sheetNo = baseExcelDto.getBaseExcelSheetDto().getSheetNo();
        String sheetName = baseExcelDto.getBaseExcelSheetDto().getSheetName();

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
            if (!CollectionUtils.isEmpty(writeBean.getHeadList())) {
                writeSheet.setHead(writeBean.getHeadList());
            }
            if (!CollectionUtils.isEmpty(writeBean.getIncludeColumnFiledNames())) {
                writeSheet.setIncludeColumnFieldNames(writeBean.getIncludeColumnFiledNames());
            }
            baseWriterExcelDto.setSheetObj(writeSheet);
        }

        try {
            excelWriter.write(Lists.newArrayList(writeBean.getTitleForTableHead()), writeSheet);
            List<List<T>> partition = Lists.partition(writeBean.getDatalist(), Constant.NUMBER_HUNDERD);
            for (List<T> list : partition) {
                excelWriter.write(list, writeSheet);
            }
            return ResultDTOUtils.success();
        } catch (Exception e) {
            log.error("doWrite::error", e);
            throw new ServiceException(ExcelExceptionEnums.IMPORT_EXCEL_EXCEPTION.getDesc());
        }
    }

    @Override
    public <T> ResultDTO<Void> doWriteFinish(ExcelWriteBean<T> writeBean) {
        Assert.notNull(writeBean, "writeBean can not be null");
        BaseExcelDTO baseExcelDto = writeBean.getBaseExcelDto();
        Assert.notNull(baseExcelDto, "baseExcelDto can not be null");
        ExcelBeanDTO baseWriterExcelDto = baseExcelDto.getExcelBeanDTO();
        if (baseWriterExcelDto.getExcelObj() instanceof ExcelWriter) {
            ExcelWriter excelWriter = (ExcelWriter)baseWriterExcelDto.getExcelObj();
            excelWriter.finish();
        }
        return ResultDTOUtils.success();
    }

    @Override
    public <T> ResultDTO<ExcelReadResultDTO<T>> readExcel(ExcelReadBean<T> excelReadBean) {

        ExcelReadResultDTO<T> excelReadResultDTO = Optional.ofNullable(excelReadBean.getExcelReadResultDTO()).orElse(new ExcelReadResultDTO<>());
        BaseExcelDTO baseExcelDTO = Optional.ofNullable(excelReadBean.getBaseExcelDTO()).orElse(new BaseExcelDTO());
        ExcelBeanDTO excelBeanDTO = Optional.ofNullable(baseExcelDTO.getExcelBeanDTO()).orElse(new ExcelBeanDTO());

        ExcelDataListener<T> excelDataListener = new ExcelDataListener<>(excelReadResultDTO);
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
                throw new ServiceException(ServiceExceptionEnum.EXCEL_FILE_PATH_ISNULL);
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
        Assert.notNull(geneTempDto, "geneTempDto can not be null");
        if (StringUtils.isEmpty(geneTempDto.getFilePath())) {
            throw new ServiceException(ServiceExceptionEnum.EXCEL_FILE_PATH_ISNULL);
        }
        List<List<String>> headList = new Vector<>();
        Map<String, String> readSetMap = geneTempDto.getReadSetMap();
        if (MapUtils.isEmpty(readSetMap)) {
            throw new ServiceException(ServiceExceptionEnum.TABLE_HEAD_ISNULL);
        }

        for (Map.Entry<String, String> entry : readSetMap.entrySet()) {
            String colName = entry.getKey();
            List<String> head = new ArrayList<>();
            head.add(Optional.ofNullable(geneTempDto.getExplainStr()).orElse(StringUtils.EMPTY));
            head.add(Optional.ofNullable(geneTempDto.getExampleMap().get(colName)).orElse(StringUtils.EMPTY));
            head.add(geneTempDto.getReadSetMap().get(colName));
            headList.add(head);
        }

        ExcelWriteBean<Map<String, String>> excelWriteBean = new ExcelWriteBean<>();
        BaseExcelDTO baseExcelDTO = new BaseExcelDTO();
        BaseExcelSheetDTO baseExcelSheetDTO = new BaseExcelSheetDTO(0);
        baseExcelSheetDTO.setSheetName("模板");
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
