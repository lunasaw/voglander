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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 导出任务管理
 *
 * @author chenzhangyue
 * @since 2024-01-30
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/exportTask")
@Tag(name = "导出任务管理", description = "导出任务管理相关接口")
public class ExportTaskController {

    @Autowired
    private ExportTaskService exportTaskService;

    @Autowired
    private ExportTaskManager exportTaskManager;

    @Autowired
    private ExportTaskWebAssembler exportTaskWebAssembler;

    @GetMapping("/get/{id}")
    @Operation(summary = "根据ID获取导出任务", description = "通过导出任务ID获取导出任务详细信息")
    @ApiResponse(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult getById(@Parameter(description = "导出任务ID") @PathVariable(value = "id") Long id) {
        ExportTaskDTO exportTaskDTO = exportTaskManager.getExportTaskDTOById(id);
        if (exportTaskDTO == null) {
            return AjaxResult.error("导出任务不存在");
        }
        ExportTaskVO exportTaskVO = ExportTaskVO.convertVO(exportTaskDTO);
        return AjaxResult.success(exportTaskVO);
    }

    @GetMapping("/get")
    @Operation(summary = "根据条件查询导出任务", description = "通过导出任务实体条件查询导出任务信息")
    public AjaxResult getByEntity(ExportTaskDO exportTask) {
        ExportTaskDTO exportTaskDTO = exportTaskManager.getExportTaskDTOByEntity(exportTask);
        if (exportTaskDTO == null) {
            return AjaxResult.error("导出任务不存在");
        }
        ExportTaskVO exportTaskVO = ExportTaskVO.convertVO(exportTaskDTO);
        return AjaxResult.success(exportTaskVO);
    }

    @GetMapping("/getBizId/{bizId}")
    @Operation(summary = "根据业务ID获取导出任务", description = "通过业务ID获取导出任务信息")
    public AjaxResult getByBizId(@Parameter(description = "业务ID") @PathVariable(value = "bizId") Long bizId) {
        ExportTaskDTO exportTaskDTO = exportTaskManager.getDTOByBizId(bizId);
        if (exportTaskDTO == null) {
            return AjaxResult.error("导出任务不存在");
        }
        ExportTaskVO exportTaskVO = ExportTaskVO.convertVO(exportTaskDTO);
        return AjaxResult.success(exportTaskVO);
    }

    @GetMapping("/list")
    @Operation(summary = "获取导出任务列表", description = "根据条件获取导出任务列表")
    public AjaxResult list(ExportTaskDO exportTask) {
        List<ExportTaskDTO> exportTaskDTOList = exportTaskManager.listExportTaskDTO(exportTask);
        List<ExportTaskVO> exportTaskVOList = exportTaskDTOList.stream()
                .map(ExportTaskVO::convertVO)
                .collect(Collectors.toList());
        return AjaxResult.success(exportTaskVOList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    @Operation(summary = "分页查询导出任务", description = "根据条件分页查询导出任务列表")
    public AjaxResult listPageByEntity(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size,
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
    @Operation(summary = "简单分页查询", description = "分页查询所有导出任务")
    public AjaxResult listPage(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size) {
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
    @Operation(summary = "创建导出任务", description = "添加新的导出任务")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult insert(@RequestBody ExportTaskCreateReq createReq) {
        // Req -> DTO (使用 Web 层转换器)
        ExportTaskDTO exportTaskDTO = exportTaskWebAssembler.toExportTaskDTO(createReq);

        // 通过 Manager 层处理业务逻辑
        Long exportTaskId = exportTaskManager.createExportTask(exportTaskDTO);

        return AjaxResult.success(exportTaskId);
    }

    @PostMapping("/insertBatch")
    @Operation(summary = "批量创建导出任务", description = "批量添加导出任务")
    public AjaxResult insertBatch(@RequestBody List<ExportTaskCreateReq> createReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<ExportTaskDTO> exportTaskDTOList = exportTaskWebAssembler.toExportTaskDTOList(createReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = exportTaskManager.batchCreateExportTask(exportTaskDTOList);

        return AjaxResult.success("成功创建 " + successCount + " 个导出任务，共 " + createReqList.size() + " 个请求");
    }

    @PutMapping("/update")
    @Operation(summary = "更新导出任务", description = "更新导出任务信息")
    public AjaxResult update(@RequestBody ExportTaskUpdateReq updateReq) {
        // Req -> DTO (使用 Web 层转换器)
        ExportTaskDTO exportTaskDTO = exportTaskWebAssembler.toExportTaskDTO(updateReq);

        Long updated = exportTaskManager.updateExportTask(exportTaskDTO);

        return AjaxResult.success(updated);
    }

    @PutMapping("/updateBatch")
    @Operation(summary = "批量更新导出任务", description = "批量更新导出任务信息")
    public AjaxResult updateBatch(@RequestBody List<ExportTaskUpdateReq> updateReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<ExportTaskDTO> exportTaskDTOList = exportTaskWebAssembler.toUpdateExportTaskDTOList(updateReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = exportTaskManager.batchUpdateExportTask(exportTaskDTOList);

        return AjaxResult.success("成功更新 " + successCount + " 个导出任务，共 " + updateReqList.size() + " 个请求");
    }

    @PutMapping("/updateStatus/{bizId}/{status}")
    @Operation(summary = "更新导出任务状态", description = "根据业务ID更新导出任务状态")
    public AjaxResult updateStatus(
        @Parameter(description = "业务ID") @PathVariable(value = "bizId") Long bizId,
        @Parameter(description = "任务状态") @PathVariable(value = "status") Integer status) {
        exportTaskManager.updateStatus(bizId, status);
        return AjaxResult.success("状态更新成功");
    }

    @PutMapping("/markCompleted/{bizId}")
    @Operation(summary = "标记任务完成", description = "将导出任务标记为已完成状态")
    public AjaxResult markAsCompleted(
        @Parameter(description = "业务ID") @PathVariable(value = "bizId") Long bizId,
        @Parameter(description = "导出文件URL") @RequestParam(value = "url") String url) {
        exportTaskManager.markAsCompleted(bizId, url);
        return AjaxResult.success("任务标记为已完成");
    }

    @PutMapping("/markError/{bizId}")
    @Operation(summary = "标记任务失败", description = "将导出任务标记为失败状态")
    public AjaxResult markAsError(@Parameter(description = "业务ID") @PathVariable(value = "bizId") Long bizId) {
        exportTaskManager.markAsError(bizId);
        return AjaxResult.success("任务标记为失败");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除导出任务", description = "根据ID删除导出任务")
    public AjaxResult deleteOne(@Parameter(description = "导出任务ID") @PathVariable(value = "id") Long id) {
        return AjaxResult.success(exportTaskService.removeById(id));
    }

    @DeleteMapping("/deleteBizId/{bizId}")
    @Operation(summary = "根据业务ID删除导出任务", description = "根据业务ID删除导出任务")
    public AjaxResult deleteByBizId(@Parameter(description = "业务ID") @PathVariable(value = "bizId") Long bizId) {
        return AjaxResult.success(exportTaskManager.deleteExportTask(bizId));
    }

    @DeleteMapping("/deleteIds")
    @Operation(summary = "批量删除导出任务", description = "根据ID列表批量删除导出任务")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        return AjaxResult.success(exportTaskService.removeBatchByIds(ids));
    }

    @GetMapping("/count")
    @Operation(summary = "统计导出任务总数", description = "获取导出任务总数量")
    public AjaxResult getAccount() {
        return AjaxResult.success(exportTaskService.count());
    }

    @GetMapping("/countByEntity")
    @Operation(summary = "按条件统计导出任务", description = "根据条件统计导出任务数量")
    public AjaxResult getAccountByEntity(ExportTaskDO exportTask) {
        QueryWrapper<ExportTaskDO> query = Wrappers.query(exportTask);
        return AjaxResult.success(exportTaskService.count(query));
    }
}
