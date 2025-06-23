package io.github.lunasaw.voglander.web.api.export;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.ExportTaskDTO;
import io.github.lunasaw.voglander.manager.manager.ExportTaskManager;
import io.github.lunasaw.voglander.manager.service.ExportTaskService;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;
import io.github.lunasaw.voglander.web.api.export.assembler.ExportTaskWebAssembler;
import io.github.lunasaw.voglander.web.api.export.req.ExportTaskCreateReq;
import io.github.lunasaw.voglander.web.api.export.req.ExportTaskUpdateReq;
import io.github.lunasaw.voglander.web.api.export.vo.ExportTaskVO;

/**
 * 导出任务管理
 *
 * @author chenzhangyue
 * @since 2024-01-30
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/exportTask")
public class ExportTaskController {

    @Autowired
    private ExportTaskService exportTaskService;

    @Autowired
    private ExportTaskManager exportTaskManager;

    @Autowired
    private ExportTaskWebAssembler exportTaskWebAssembler;

    @GetMapping("/get/{id}")
    public AjaxResult getById(@PathVariable(value = "id") Long id) {
        ExportTaskDTO exportTaskDTO = exportTaskManager.getExportTaskDTOById(id);
        if (exportTaskDTO == null) {
            return AjaxResult.error("导出任务不存在");
        }
        ExportTaskVO exportTaskVO = ExportTaskVO.convertVO(exportTaskDTO);
        return AjaxResult.success(exportTaskVO);
    }

    @GetMapping("/get")
    public AjaxResult getByEntity(ExportTaskDO exportTask) {
        ExportTaskDTO exportTaskDTO = exportTaskManager.getExportTaskDTOByEntity(exportTask);
        if (exportTaskDTO == null) {
            return AjaxResult.error("导出任务不存在");
        }
        ExportTaskVO exportTaskVO = ExportTaskVO.convertVO(exportTaskDTO);
        return AjaxResult.success(exportTaskVO);
    }

    @GetMapping("/getBizId/{bizId}")
    public AjaxResult getByBizId(@PathVariable(value = "bizId") Long bizId) {
        ExportTaskDTO exportTaskDTO = exportTaskManager.getDTOByBizId(bizId);
        if (exportTaskDTO == null) {
            return AjaxResult.error("导出任务不存在");
        }
        ExportTaskVO exportTaskVO = ExportTaskVO.convertVO(exportTaskDTO);
        return AjaxResult.success(exportTaskVO);
    }

    @GetMapping("/list")
    public AjaxResult list(ExportTaskDO exportTask) {
        List<ExportTaskDTO> exportTaskDTOList = exportTaskManager.listExportTaskDTO(exportTask);
        List<ExportTaskVO> exportTaskVOList = exportTaskDTOList.stream()
                .map(ExportTaskVO::convertVO)
                .collect(Collectors.toList());
        return AjaxResult.success(exportTaskVOList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    public AjaxResult listPageByEntity(
        @PathVariable(value = "page") int page,
        @PathVariable(value = "size") int size,
        ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = Wrappers.query(exportTask);
        Page<ExportTaskDTO> pageInfo = exportTaskManager.pageQuery(page, size, query);

        // 转换为 VO 模型
        List<ExportTaskVO> exportTaskVOList = pageInfo.getRecords().stream()
                .map(ExportTaskVO::convertVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        Page<ExportTaskVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(exportTaskVOList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return AjaxResult.success(resultPage);
    }

    @GetMapping("/pageList/{page}/{size}")
    public AjaxResult listPage(
        @PathVariable(value = "page") int page,
        @PathVariable(value = "size") int size) {
        Page<ExportTaskDTO> pageInfo = exportTaskManager.pageQuerySimple(page, size);

        // 转换为 VO 模型
        List<ExportTaskVO> exportTaskVOList = pageInfo.getRecords().stream()
                .map(ExportTaskVO::convertVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        Page<ExportTaskVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(exportTaskVOList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return AjaxResult.success(resultPage);
    }

    @PostMapping("/insert")
    public AjaxResult insert(@RequestBody ExportTaskCreateReq createReq) {
        // Req -> DTO (使用 Web 层转换器)
        ExportTaskDTO exportTaskDTO = exportTaskWebAssembler.toExportTaskDTO(createReq);

        // 通过 Manager 层处理业务逻辑
        Long exportTaskId = exportTaskManager.createExportTask(exportTaskDTO);

        return AjaxResult.success(exportTaskId);
    }

    @PostMapping("/insertBatch")
    public AjaxResult insertBatch(@RequestBody List<ExportTaskCreateReq> createReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<ExportTaskDTO> exportTaskDTOList = exportTaskWebAssembler.toExportTaskDTOList(createReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = exportTaskManager.batchCreateExportTask(exportTaskDTOList);

        return AjaxResult.success("成功创建 " + successCount + " 个导出任务，共 " + createReqList.size() + " 个请求");
    }

    @PutMapping("/update")
    public AjaxResult update(@RequestBody ExportTaskUpdateReq updateReq) {
        // Req -> DTO (使用 Web 层转换器)
        ExportTaskDTO exportTaskDTO = exportTaskWebAssembler.toExportTaskDTO(updateReq);

        Long updated = exportTaskManager.updateExportTask(exportTaskDTO);

        return AjaxResult.success(updated);
    }

    @PutMapping("/updateBatch")
    public AjaxResult updateBatch(@RequestBody List<ExportTaskUpdateReq> updateReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<ExportTaskDTO> exportTaskDTOList = exportTaskWebAssembler.toUpdateExportTaskDTOList(updateReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = exportTaskManager.batchUpdateExportTask(exportTaskDTOList);

        return AjaxResult.success("成功更新 " + successCount + " 个导出任务，共 " + updateReqList.size() + " 个请求");
    }

    @PutMapping("/updateStatus/{bizId}/{status}")
    public AjaxResult updateStatus(
        @PathVariable(value = "bizId") Long bizId,
        @PathVariable(value = "status") Integer status) {
        exportTaskManager.updateStatus(bizId, status);
        return AjaxResult.success("状态更新成功");
    }

    @PutMapping("/markCompleted/{bizId}")
    public AjaxResult markAsCompleted(
        @PathVariable(value = "bizId") Long bizId,
        @RequestParam(value = "url") String url) {
        exportTaskManager.markAsCompleted(bizId, url);
        return AjaxResult.success("任务标记为已完成");
    }

    @PutMapping("/markError/{bizId}")
    public AjaxResult markAsError(@PathVariable(value = "bizId") Long bizId) {
        exportTaskManager.markAsError(bizId);
        return AjaxResult.success("任务标记为失败");
    }

    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteOne(@PathVariable(value = "id") Long id) {
        return AjaxResult.success(exportTaskService.removeById(id));
    }

    @DeleteMapping("/deleteBizId/{bizId}")
    public AjaxResult deleteByBizId(@PathVariable(value = "bizId") Long bizId) {
        return AjaxResult.success(exportTaskManager.deleteExportTask(bizId));
    }

    @DeleteMapping("/deleteIds")
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
