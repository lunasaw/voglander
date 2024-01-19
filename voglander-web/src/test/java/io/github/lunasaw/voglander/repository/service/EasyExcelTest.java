package io.github.lunasaw.voglander.repository.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import io.github.lunasaw.voglander.client.domain.excel.dto.*;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelReadReq;
import io.github.lunasaw.voglander.client.service.excel.ExcelInnerService;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call.ExcelKey;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call.ExcelMerge;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
        List<List<String>> lists = List.of(List.of("2", "4"), List.of("3"), List.of("4"));

        List<List<String>> headList = new ArrayList<>();
        List<String> head = new ArrayList<>();
        head.add("表头1");
        headList.add(head);

        List<String> head2 = new ArrayList<>();
        head2.add("表头2");
        headList.add(head2);

        List<String> head3 = new ArrayList<>();
        head3.add("表头3");
        head3.add("表头4");
        headList.add(head3);

        System.out.println(headList);

        excelWriteBean.setHeadList(headList);
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
        list.add(new DemoData());
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
    public static class DemoData {
        @ExcelProperty("字符串标题")
        @ExcelMerge
        private String string;
        @ExcelProperty("日期标题")
        @ExcelMerge
        private Date   date;
        @ExcelProperty("数字标题")
        @ColumnWidth(50)
        private Double doubleData;
        /**
         * 忽略这个字段
         */
        @ExcelIgnore
        @ExcelProperty("数字标题")
        private String ignore;
    }

    @Data
    public static class AfterSaleExcelListVo implements Serializable {

        /**
         * 售后订单编号
         */
        @ExcelProperty(value = {"销售售后单/铺货售后单导出", "售后订单编号"})
        @ExcelKey("saleBillId")
        @ExcelMerge
        private String  saleBillId;

        /**
         * 售后类型 1=退货退款,2=仅退款，3=补寄
         */
        @ExcelProperty(value = {"销售售后单/铺货售后单导出", "售后类型"})
        @ExcelMerge
        private Integer saleType;

    }
}
