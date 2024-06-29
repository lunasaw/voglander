package io.github.lunasaw.voglander.manager.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luna.common.check.AssertUtil;
import com.luna.common.constant.Constant;
import com.luna.common.exception.BaseException;

import io.github.lunasaw.voglander.manager.domaon.dto.ExportTaskDTO;
import io.github.lunasaw.voglander.manager.service.ExportTaskService;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;

/**
 * @author luna
 * @date 2024/1/29
 */
@Component
public class ExportTaskManager {

    @Autowired
    private ExportTaskService exportTaskService;

    public ExportTaskDTO getByBizId(Long bizId) {
        AssertUtil.notNull(bizId, BaseException.PARAMETER_ERROR, "bizId不能为空");
        LambdaQueryWrapper<ExportTaskDO> query = Wrappers.lambdaQuery();
        query.eq(ExportTaskDO::getBizId, bizId).last("limit 1");
        return ExportTaskDTO.do2Dto(exportTaskService.getOne(query));
    }

    public List<ExportTaskDO> listAll() {
        return listAll(new ExportTaskDO());
    }

    public List<ExportTaskDO> listAll(ExportTaskDO exportTaskDO) {
        int pageNum = Constant.NUMBER_ONE;
        int pageSize = Constant.NUMBER_FIFTY;
        List<ExportTaskDO> list = new ArrayList<>();
        while (true) {
            Page<ExportTaskDO> page = listPage(pageNum, pageSize, exportTaskDO);
            if (CollectionUtils.isEmpty(page.getRecords())) {
                break;
            }
            list.addAll(page.getRecords());
            pageNum++;
        }
        return list;
    }

    public Page<ExportTaskDO> listPage(Integer page, Integer pageSize, ExportTaskDO memberExport) {
        if (page == null) {
            page = 1;
        }
        if (pageSize == null) {
            pageSize = 20;
        }
        QueryWrapper<ExportTaskDO> query = Wrappers.query(memberExport);
        return exportTaskService.page(new Page<>(page, pageSize), query);
    }
}
