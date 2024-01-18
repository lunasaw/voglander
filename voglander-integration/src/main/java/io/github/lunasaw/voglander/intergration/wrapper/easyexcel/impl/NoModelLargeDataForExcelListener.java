package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.impl;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import com.alibaba.fastjson.JSON;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadDTO;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelReadReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

/**
 * 直接用map接收数据
 *
 * @author weidian
 */
@Slf4j
public class NoModelLargeDataForExcelListener extends AnalysisEventListener<Map<Integer, String>> {
    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    private ExcelReadDTO excelReadDTO = null;
    private ExcelReadReq excelReadReq = null;

    private int          count        = 0;

    public NoModelLargeDataForExcelListener() {

    }

    public NoModelLargeDataForExcelListener(ExcelReadDTO excelReadDTO, ExcelReadReq excelReadReq) {
        this.excelReadDTO = excelReadDTO;
        this.excelReadReq = excelReadReq;
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        System.out.println(JSON.toJSONString(data));
        if (count == 0) {
            dealHeadMap(data);
            // log.info("First row:{}", JSON.toJSONString(data));
        } else {
            if (MapUtils.isNotEmpty(excelReadDTO.getHeadMap())) {
                Map<Integer, String> resultMap = new HashMap<>();
                for (Integer index : excelReadDTO.getHeadMap().keySet()) {
                    resultMap.put(index, data.get(index));
                }
                excelReadDTO.getReadResultMap().add(resultMap);
            }
        }
        count++;
        if (count % 100000 == 0) {
            log.info("Already read:{}", count);
        }
    }

    private void dealHeadMap(Map<Integer, String> data) {
        for (Integer indexNo : data.keySet()) {
            String viewName = data.get(indexNo).trim();
            excelReadDTO.getHeadMap().put(indexNo, viewName);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("Large row count:{}", count);
    }
}
