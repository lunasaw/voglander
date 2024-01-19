package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.call;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * @author luna
 * @date 2024/1/19
 */
public class HeaderMergeStrategy extends AbstractMergeStrategy {
    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (relativeRowIndex == 0) {
            // 合并第一行所有列
            CellRangeAddress cellRangeAddress = new CellRangeAddress(1, 2, 0, 2);

            sheet.addMergedRegionUnsafe(cellRangeAddress);
        }
    }
}

