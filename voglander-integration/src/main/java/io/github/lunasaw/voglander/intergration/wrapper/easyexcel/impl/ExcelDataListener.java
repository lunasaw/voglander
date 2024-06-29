package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.Assert;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;

import io.github.lunasaw.voglander.client.domain.excel.dto.ExcelReadResultDTO;
import io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call.SaveDataFunction;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 */
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class ExcelDataListener<T> implements ReadListener<T> {

    /**
     * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
     */
    public static final int       BATCH_COUNT    = 100;
    /**
     * 缓存的数据
     */
    private List<T>               cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    /**
     * 数据入库
     */
    private SaveDataFunction<T>   saveDataFunction;

    /**
     * 数据计数
     */
    private AtomicInteger         count          = new AtomicInteger(0);

    /**
     * 返回数据
     */
    private ExcelReadResultDTO<T> excelReadResultDTO;

    public ExcelDataListener(SaveDataFunction<T> saveDataFunction) {
        this.saveDataFunction = saveDataFunction;
    }

    public ExcelDataListener(ExcelReadResultDTO<T> excelReadResultDTO) {
        this.excelReadResultDTO = excelReadResultDTO;
        Assert.notNull(this.excelReadResultDTO.getReadResultList(), "readResultList can not be null");
    }

    public ExcelDataListener(SaveDataFunction<T> saveDataFunction, ExcelReadResultDTO<T> excelReadResultDTO) {
        this.saveDataFunction = saveDataFunction;
        this.excelReadResultDTO = excelReadResultDTO;
        Assert.notNull(this.excelReadResultDTO.getReadResultList(), "readResultList can not be null");
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data one row value. Is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(T data, AnalysisContext context) {
        cachedDataList.add(data);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }

        List<T> readResultList = excelReadResultDTO.getReadResultList();
        readResultList.add(data);

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
        }
    }

}