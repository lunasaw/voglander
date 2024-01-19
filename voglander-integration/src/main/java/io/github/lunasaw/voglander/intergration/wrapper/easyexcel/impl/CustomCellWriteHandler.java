package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.impl;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import lombok.Data;
import org.apache.poi.ss.usermodel.Cell;

import java.util.List;

/**
 * @author luna
 * @date 2024/1/19
 */
@Data
public class CustomCellWriteHandler implements CellWriteHandler {

    private String title;

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<WriteCellData<?>> cellDataList, Cell cell,
        Head head, Integer relativeRowIndex, Boolean isHead) {

        CellWriteHandler.super.afterCellDispose(writeSheetHolder, writeTableHolder, cellDataList, cell, head, relativeRowIndex, isHead);
    }
}
