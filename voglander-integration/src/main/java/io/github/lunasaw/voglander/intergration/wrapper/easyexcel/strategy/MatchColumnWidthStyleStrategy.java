package io.github.lunasaw.voglander.intergration.wrapper.easyexcel.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.luna.common.constant.Constant;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.ss.usermodel.Cell;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;

import lombok.Data;

/**
 * Take the width of the longest column as the width.
 * <p>
 * This is not very useful at the moment, for example if you have Numbers it will cause a newline.And the length is not
 * exactly the same as the actual length.
 *
 * @author Jiaju Zhuang
 */
@Data
public class MatchColumnWidthStyleStrategy extends AbstractColumnWidthStyleStrategy {

    private Map<Integer, Integer> columnWidthMap = new HashMap<>();

    private Integer               width          = 50;
    private Integer               charLength     = 6;

    @Override
    protected void setColumnWidth(WriteSheetHolder writeSheetHolder, List<WriteCellData<?>> cellDataList, Cell cell,
        Head head,
        Integer relativeRowIndex, Boolean isHead) {
        boolean needSetWidth = isHead || !CollectionUtils.isEmpty(cellDataList);
        if (!needSetWidth) {
            return;
        }
        if (MapUtils.isEmpty(columnWidthMap)) {
            return;
        }
        Integer actWith = columnWidthMap.get(cell.getColumnIndex());
        if (actWith != null) {
            actWith = (actWith * charLength) / 50 + 1;
        } else {
            // 默认宽度100
            actWith = Constant.NUMBER_HUNDERD;
        }
        writeSheetHolder.getSheet().setColumnWidth(cell.getColumnIndex(), actWith * 256);
    }

}
