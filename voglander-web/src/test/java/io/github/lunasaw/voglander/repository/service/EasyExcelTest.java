package io.github.lunasaw.voglander.repository.service;

import com.alibaba.fastjson.JSON;
import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadDTO;
import io.github.lunasaw.voglander.client.domain.excel.req.ExcelReadReq;
import io.github.lunasaw.voglander.client.service.excel.ExcelInnerService;
import io.github.lunasaw.voglander.web.ApplicationWeb;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
}
