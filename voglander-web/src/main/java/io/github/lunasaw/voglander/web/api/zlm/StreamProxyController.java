package io.github.lunasaw.voglander.web.api.zlm;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.voglander.web.api.zlm.assembler.StreamProxyWebAssembler;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyListResp;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * 拉流代理管理
 *
 * @author luna
 * @since 2025-01-23
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/proxy")
@Tag(name = "拉流代理管理", description = "拉流代理增删改查等相关接口")
public class StreamProxyController {

    @Autowired
    private StreamProxyManager      streamProxyManager;

    @Autowired
    private StreamProxyWebAssembler streamProxyWebAssembler;

    @GetMapping("/get/{id}")
    @Operation(summary = "根据ID获取代理", description = "通过数据库主键ID获取拉流代理详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult<StreamProxyVO> getById(@Parameter(description = "代理数据库ID") @PathVariable(value = "id") Long id) {
        StreamProxyDTO streamProxyDTO = streamProxyManager.getById(id);
        if (streamProxyDTO == null) {
            return AjaxResult.error("代理不存在");
        }

        // 直接转换DTO为VO
        StreamProxyVO vo = streamProxyWebAssembler.dtoToVo(streamProxyDTO);
        return AjaxResult.success(vo);
    }

    @GetMapping("/getByKey/{proxyKey}")
    @Operation(summary = "根据代理key获取代理", description = "通过代理key获取拉流代理信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public AjaxResult<StreamProxyVO> getByProxyKey(@Parameter(description = "代理key") @PathVariable(value = "proxyKey") String proxyKey) {
        // 构建查询条件
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setProxyKey(proxyKey);

        StreamProxyDTO streamProxyDTO = streamProxyManager.get(queryDTO);
        if (streamProxyDTO == null) {
            return AjaxResult.error("代理不存在");
        }

        StreamProxyVO vo = streamProxyWebAssembler.dtoToVo(streamProxyDTO);
        return AjaxResult.success(vo);
    }

    @GetMapping("/getByAppAndStream")
    @Operation(summary = "根据应用和流名获取代理", description = "通过应用名和流名获取拉流代理信息")
    public AjaxResult<StreamProxyVO> getByAppAndStream(
        @Parameter(description = "应用名称") @RequestParam String app,
        @Parameter(description = "流ID") @RequestParam String stream) {
        // 构建查询条件
        StreamProxyDTO queryDTO = new StreamProxyDTO();
        queryDTO.setApp(app);
        queryDTO.setStream(stream);

        StreamProxyDTO streamProxyDTO = streamProxyManager.get(queryDTO);
        if (streamProxyDTO == null) {
            return AjaxResult.error("代理不存在");
        }

        StreamProxyVO vo = streamProxyWebAssembler.dtoToVo(streamProxyDTO);
        return AjaxResult.success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页获取代理列表", description = "分页获取拉流代理列表")
    public AjaxResult<StreamProxyListResp> page(
        @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "页大小，默认10") @RequestParam(defaultValue = "10") int size) {
        Page<StreamProxyDTO> pageResult = streamProxyManager.getPage(null, page, size);

        List<StreamProxyVO> voList = pageResult.getRecords().stream()
            .map(streamProxyWebAssembler::dtoToVo)
            .collect(Collectors.toList());

        StreamProxyListResp resp = new StreamProxyListResp();
        resp.setTotal(pageResult.getTotal());
        resp.setItems(voList);

        return AjaxResult.success(resp);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "更新拉流代理", description = "更新已存在的拉流代理")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Boolean> update(
        @Parameter(description = "代理ID") @PathVariable Long id,
        @Valid @RequestBody StreamProxyUpdateReq updateReq) {

        // 检查代理是否存在
        StreamProxyDTO dto = streamProxyManager.getById(id);
        if (dto == null) {
            return AjaxResult.error("代理不存在");
        }

        // 更新字段
        if (updateReq.getDescription() != null) {
            dto.setDescription(updateReq.getDescription());
        }
        if (updateReq.getStatus() != null) {
            dto.setStatus(updateReq.getStatus());
        }
        if (updateReq.getEnabled() != null) {
            dto.setEnabled(updateReq.getEnabled());
        }
        if (updateReq.getExtend() != null) {
            dto.setExtend(updateReq.getExtend());
        }

        Boolean proxyId = streamProxyManager.updateStreamProxy(dto, "更新拉流代理");
        return AjaxResult.success(proxyId);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除拉流代理", description = "删除指定的拉流代理")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult<Void> delete(@Parameter(description = "代理ID") @PathVariable Long id) {
        boolean success = streamProxyManager.deleteStreamProxyById(id, "删除拉流代理");
        if (success) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    @DeleteMapping("/deleteByKey/{proxyKey}")
    @Operation(summary = "根据代理key删除代理", description = "根据代理key删除拉流代理")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult<Void> deleteByKey(@Parameter(description = "代理key") @PathVariable String proxyKey) {
        boolean success = streamProxyManager.deleteByProxyKey(proxyKey, "根据key删除拉流代理");
        if (success) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    @PutMapping("/updateStatus/{id}")
    @Operation(summary = "更新代理状态", description = "更新拉流代理的状态")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Boolean> updateStatus(
        @Parameter(description = "代理ID") @PathVariable Long id,
        @Parameter(description = "状态 1启用 0禁用") @RequestParam Integer status) {

        // 检查代理是否存在
        StreamProxyDTO existingProxy = streamProxyManager.getById(id);
        if (existingProxy == null) {
            return AjaxResult.error("代理不存在");
        }

        // 构建更新DTO - 仅更新状态字段
        StreamProxyDTO updateDTO = new StreamProxyDTO();
        updateDTO.setId(id);
        updateDTO.setStatus(status);

        // 使用Manager进行状态更新
        Boolean success = streamProxyManager.updateStreamProxy(updateDTO, "更新代理状态");
        if (success) {
            return AjaxResult.success(success);
        } else {
            return AjaxResult.error("更新状态失败");
        }
    }
}