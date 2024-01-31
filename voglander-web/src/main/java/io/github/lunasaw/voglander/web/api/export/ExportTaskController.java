package io.github.lunasaw.voglander.web.api.export;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.service.ExportTaskService;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;

/**
 * @Author: chenzhangyue
 * @CreateTime: 2024-01-30 14:09:40
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/exportTask")
public class ExportTaskController {

    @Autowired
    private ExportTaskService exportTaskService;

    @GetMapping("/get/{id}")
    public AjaxResult getById(@PathVariable(value = "id") Long id) {
        ExportTaskDO exportTask = exportTaskService.getById(id);
        return AjaxResult.success(exportTask);
    }

    @GetMapping("/get")
    public AjaxResult getByEntity(ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = Wrappers.query(exportTask).last("limit 1");
        return AjaxResult.success(exportTaskService.getOne(query));
    }

    @GetMapping("/list")
    public AjaxResult list(ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = Wrappers.query(exportTask);
        List<ExportTaskDO> exportTaskList = exportTaskService.list(query);
        return AjaxResult.success(exportTaskList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    public AjaxResult listPageByEntity(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size, ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = Wrappers.query(exportTask);
        Page<ExportTaskDO> queryPage = new Page<>(page, size);
        Page<ExportTaskDO> pageInfo = exportTaskService.page(queryPage, query);
        return AjaxResult.success(pageInfo);
    }

    @GetMapping("/pageList/{page}/{size}")
    public AjaxResult listPage(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size) {
        Page<ExportTaskDO> queryPage = new Page<>(page, size);
        Page<ExportTaskDO> pageInfo = exportTaskService.page(queryPage);
        return AjaxResult.success(pageInfo);
    }

    @PostMapping("/insert")
    public AjaxResult insert(@RequestBody ExportTaskDO exportTask) {
        exportTaskService.save(exportTask);
        return AjaxResult.success(exportTask);
    }

    @PostMapping("/insertBatch")
    public AjaxResult insert(@RequestBody List<ExportTaskDO> list) {
        boolean saved = exportTaskService.saveBatch(list);
        return AjaxResult.success(saved);
    }

    @PutMapping("/update")
    public AjaxResult update(@RequestBody ExportTaskDO exportTask) {
        UpdateWrapper<ExportTaskDO> update = Wrappers.update(exportTask);
        return AjaxResult.success(exportTaskService.update(update));
    }

    @PutMapping("/updateBatch")
    public AjaxResult update(@RequestBody List<ExportTaskDO> list) {
        return AjaxResult.success(exportTaskService.updateBatchById(list));
    }

    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteOne(@PathVariable(value = "id") Long id) {
        return AjaxResult.success(exportTaskService.removeById(id));
    }

    @DeleteMapping("/deleteByEntity")
    public AjaxResult deleteOne(@RequestBody ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = Wrappers.query(exportTask);
        return AjaxResult.success(exportTaskService.remove(query));
    }

    @DeleteMapping("/delete")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        return AjaxResult.success(exportTaskService.removeBatchByIds(ids));
    }

    @GetMapping("/count")
    public AjaxResult getAccount() {
        return AjaxResult.success(exportTaskService.count());
    }

    @GetMapping("/countByEntity")
    public AjaxResult getAccountByEntity(ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = Wrappers.query(exportTask);
        return AjaxResult.success(exportTaskService.count(query));
    }
}
