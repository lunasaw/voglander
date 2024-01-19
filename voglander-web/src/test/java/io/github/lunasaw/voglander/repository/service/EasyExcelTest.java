package io.github.lunasaw.voglander.repository.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableBiMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.OnceAbsoluteMerge;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;

import io.github.lunasaw.voglander.client.domain.excel.dto.*;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelReadReq;
import io.github.lunasaw.voglander.client.service.excel.ExcelInnerService;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.*;

/**
 * @author luna
 * @date 2024/1/18
 */
@SpringBootTest(classes = ApplicationWeb.class)
public class EasyExcelTest {

    @Autowired
    private ExcelInnerService excelInnerService;

    @SneakyThrows
    @Test
    public void atest() {
        ExcelReadReq excelReadReq = new ExcelReadReq();
        excelReadReq.setHeadRowNumber(0);
        excelReadReq.setFilePath("/Users/weidian/Downloads/live-63e500000178af7d04c30a2064e0.xlsx");
        ExcelReadDTO excelReadDTO = excelInnerService.readExcel(excelReadReq);
        List<Map<Integer, String>> readResultMap = excelReadDTO.getReadResultMap();
        System.out.println(JSON.toJSONString(excelReadDTO.getHeadMap()));
        System.out.println(JSON.toJSONString(readResultMap));
    }

    @Test
    public void btest() {

        ExcelWriteBean<DemoData> excelWriteBean = new ExcelWriteBean<>();
        excelWriteBean.setDatalist(data());
        excelWriteBean.setTClass(DemoData.class);

        BaseExcelDTO baseExcelDTO = new BaseExcelDTO();

        ExcelWriterDTO excelWriterDTO = new ExcelWriterDTO();
        baseExcelDTO.setExcelWriterDTO(excelWriterDTO);

        BaseExcelSheetDTO baseExcelSheetDTO = new BaseExcelSheetDTO(0);
        baseExcelDTO.setBaseExcelSheetDto(baseExcelSheetDTO);

        excelWriteBean.setBaseExcelDto(baseExcelDTO);
        excelWriteBean.setTempPath("/Users/weidian/Downloads/live-3.xlsx");
        excelInnerService.doWrite(excelWriteBean);

        excelInnerService.doWrite(excelWriteBean);

        excelInnerService.doWrite(excelWriteBean);

        excelInnerService.doWriteFinish(excelWriteBean);
    }

    @Test
    public void dtest() {
        GeneTempDTO geneTempDTO = new GeneTempDTO();
        geneTempDTO.setFilePath("/Users/weidian/Downloads/live-4.xlsx");
        geneTempDTO.setExampleMap(ImmutableBiMap.of("213", "模版数据1", "24", "模版数据"));
        geneTempDTO.setExplainStr("33dfdsafadfa");
        geneTempDTO.setReadSetMap(ImmutableBiMap.of("213", "2", "24", "3333"));
        excelInnerService.geneTempFile(geneTempDTO);
    }

    @Test
    public void simpleWrite() {
        // 注意 simpleWrite在数据量不大的情况下可以使用（5000以内，具体也要看实际情况），数据量大参照 重复多次写入

        // 写法1 JDK8+
        // since: 3.0.0-beta1
        String fileName = "/Users/weidian/Downloads/" + "simpleWrite" + System.currentTimeMillis() + ".xlsx";
        // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        // 如果这里想使用03 则 传入excelType参数即可
        EasyExcel.write(fileName, DemoData.class)
            .sheet("模板")
            .doWrite(() -> {
                // 分页查询数据
                return data();
            });

        // 写法3
        fileName = "/Users/weidian/Downloads/" + "simpleWrite" + System.currentTimeMillis() + ".xlsx";
        // 这里 需要指定写用哪个class去写
        try (ExcelWriter excelWriter = EasyExcel.write(fileName).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet(0, "模板").build();
            excelWriter.write(data(), writeSheet);
        }
    }

    @Test
    public void ctest() {
        String fileName;
        // 写法2
        fileName = "/Users/weidian/Downloads/" + "simpleWrite" + System.currentTimeMillis() + ".xlsx";
        // 这里 需要指定写用哪个class去写，然后写到第一个sheet，名字为模板 然后文件流会自动关闭
        // 如果这里想使用03 则 传入excelType参数即可
        EasyExcel.write(fileName).sheet("模板").doWrite(data());
    }

    private List<DemoData> data() {
        List<DemoData> list = ListUtils.newArrayList();
        for (int i = 0; i < 10; i++) {
            DemoData data = new DemoData();
            data.setString("字符串" + i + "\uD83D\uDE02");
            data.setDate(new Date());
            data.setDoubleData(0.56);
            list.add(data);
        }

        return list;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @OnceAbsoluteMerge(firstRowIndex = 0, lastRowIndex = 1, firstColumnIndex = 0, lastColumnIndex = 2)
    public static class DemoData {
        @ExcelProperty({"主标题", "字符串标题"})
        private String string;
        @ExcelProperty({"主标题", "日期标题"})
        private Date   date;
        @ExcelProperty({"主标题", "数字标题"})
        private Double doubleData;
    }

}
