package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections4.MapUtils;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson.JSON;

import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadDTO;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call.SaveDataFunction;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author weidian
 */
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class ExcelDataListener<K, T> implements ReadListener<Map<K, T>> {

    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    public static final int        BATCH_COUNT    = 100;
    /**
     * 缓存的数据
     */
    private List<Map<K, T>>        cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    /**
     * 数据入库
     */
    private SaveDataFunction<K, T> saveDataFunction;

    /**
     * 数据计数
     */
    private AtomicInteger          count          = new AtomicInteger(0);

    /**
     * 返回数据
     */
    private ExcelReadDTO           excelReadDTO;

    public ExcelDataListener(SaveDataFunction<K, T> saveDataFunction) {
        this.saveDataFunction = saveDataFunction;
    }

    public ExcelDataListener(ExcelReadDTO excelReadDTO) {
        this.excelReadDTO = excelReadDTO;
    }

    public ExcelDataListener(SaveDataFunction<K, T> saveDataFunction, ExcelReadDTO excelReadDTO) {
        this.saveDataFunction = saveDataFunction;
        this.excelReadDTO = excelReadDTO;
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data one row value. Is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(Map<K, T> data, AnalysisContext context) {
        cachedDataList.add(data);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }

        if (count.get() == 0) {
            dealHeadMap((Map<Integer, String>)data);
        } else {
            if (MapUtils.isNotEmpty(excelReadDTO.getHeadMap())) {
                Map<Integer, String> resultMap = new HashMap<>();
                for (Integer index : excelReadDTO.getHeadMap().keySet()) {
                    resultMap.put(index, (String)data.get(index));
                }
                excelReadDTO.getReadResultMap().add(resultMap);
            }
        }

        count.incrementAndGet();
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
        log.info("所有数据解析完成！");
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", cachedDataList.size());
        if (saveDataFunction != null) {
            int save = saveDataFunction.save(cachedDataList);
            log.info("{}条存储数据库成功！", save);
        } else {
            log.info("未实现保存方法！");
        }
    }

    private void dealHeadMap(Map<Integer, String> data) {
        for (Integer indexNo : data.keySet()) {
            String viewName = data.get(indexNo).trim();
            excelReadDTO.getHeadMap().put(indexNo, viewName);
        }
    }

}