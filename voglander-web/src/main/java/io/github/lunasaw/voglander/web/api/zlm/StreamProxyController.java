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
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.web.api.zlm.assembler.StreamProxyWebAssembler;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
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
    public AjaxResult getById(@Parameter(description = "代理数据库ID") @PathVariable(value = "id") Long id) {
        StreamProxyDO streamProxyDO = streamProxyManager.getById(id);
        if (streamProxyDO == null) {
            return AjaxResult.error("代理不存在");
        }

        // 转换为DTO再转换为VO
        StreamProxyDTO dto = streamProxyManager.doToDto(streamProxyDO);
        StreamProxyVO vo = streamProxyWebAssembler.dtoToVo(dto);
        return AjaxResult.success(vo);
    }

    @GetMapping("/getByKey/{proxyKey}")
    @Operation(summary = "根据代理key获取代理", description = "通过代理key获取拉流代理信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public AjaxResult getByProxyKey(@Parameter(description = "代理key") @PathVariable(value = "proxyKey") String proxyKey) {
        StreamProxyDO streamProxyDO = streamProxyManager.getByProxyKey(proxyKey);
        if (streamProxyDO == null) {
            return AjaxResult.error("代理不存在");
        }

        StreamProxyDTO dto = streamProxyManager.doToDto(streamProxyDO);
        StreamProxyVO vo = streamProxyWebAssembler.dtoToVo(dto);
        return AjaxResult.success(vo);
    }

    @GetMapping("/getByAppAndStream")
    @Operation(summary = "根据应用和流名获取代理", description = "通过应用名和流名获取拉流代理信息")
    public AjaxResult getByAppAndStream(
        @Parameter(description = "应用名称") @RequestParam String app,
        @Parameter(description = "流ID") @RequestParam String stream) {
        StreamProxyDO streamProxyDO = streamProxyManager.getByAppAndStream(app, stream);
        if (streamProxyDO == null) {
            return AjaxResult.error("代理不存在");
        }

        StreamProxyDTO dto = streamProxyManager.doToDto(streamProxyDO);
        StreamProxyVO vo = streamProxyWebAssembler.dtoToVo(dto);
        return AjaxResult.success(vo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页获取代理列表", description = "分页获取拉流代理列表")
    public AjaxResult page(
        @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "页大小，默认10") @RequestParam(defaultValue = "10") int size) {
        Page<StreamProxyDO> pageResult = streamProxyManager.getProxyPage(page, size);

        List<StreamProxyVO> voList = pageResult.getRecords().stream()
            .map(streamProxyDO -> {
                StreamProxyDTO dto = streamProxyManager.doToDto(streamProxyDO);
                return streamProxyWebAssembler.dtoToVo(dto);
            })
            .collect(Collectors.toList());

        StreamProxyListResp resp = new StreamProxyListResp();
        resp.setTotal(pageResult.getTotal());
        resp.setItems(voList);

        return AjaxResult.success(resp);
    }

    @PostMapping("/create")
    @Operation(summary = "创建拉流代理", description = "创建新的拉流代理")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult create(@Valid @RequestBody StreamProxyCreateReq createReq) {
        StreamProxyDTO dto = streamProxyWebAssembler.createReqToDto(createReq);
        Long proxyId = streamProxyManager.createStreamProxy(dto);
        return AjaxResult.success(proxyId);
    }

    @PutMapping("/update/{id}")
    @Operation(summary = "更新拉流代理", description = "更新已存在的拉流代理")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult update(
        @Parameter(description = "代理ID") @PathVariable Long id,
        @Valid @RequestBody StreamProxyUpdateReq updateReq) {

        // 检查代理是否存在
        StreamProxyDO existingProxy = streamProxyManager.getById(id);
        if (existingProxy == null) {
            return AjaxResult.error("代理不存在");
        }

        // 更新字段
        if (updateReq.getDescription() != null) {
            existingProxy.setDescription(updateReq.getDescription());
        }
        if (updateReq.getStatus() != null) {
            existingProxy.setStatus(updateReq.getStatus());
        }
        if (updateReq.getEnabled() != null) {
            existingProxy.setEnabled(updateReq.getEnabled());
        }
        if (updateReq.getExtend() != null) {
            existingProxy.setExtend(updateReq.getExtend());
        }

        Long proxyId = streamProxyManager.updateStreamProxy(existingProxy, "更新拉流代理");
        return AjaxResult.success(proxyId);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除拉流代理", description = "删除指定的拉流代理")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult delete(@Parameter(description = "代理ID") @PathVariable Long id) {
        boolean success = streamProxyManager.deleteStreamProxy(id, "删除拉流代理");
        if (success) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    @DeleteMapping("/deleteByKey/{proxyKey}")
    @Operation(summary = "根据代理key删除代理", description = "根据代理key删除拉流代理")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult deleteByKey(@Parameter(description = "代理key") @PathVariable String proxyKey) {
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
    public AjaxResult updateStatus(
        @Parameter(description = "代理ID") @PathVariable Long id,
        @Parameter(description = "状态 1启用 0禁用") @RequestParam Integer status) {

        StreamProxyDO existingProxy = streamProxyManager.getById(id);
        if (existingProxy == null) {
            return AjaxResult.error("代理不存在");
        }

        existingProxy.setStatus(status);
        Long proxyId = streamProxyManager.updateStreamProxy(existingProxy, "更新代理状态");
        return AjaxResult.success(proxyId);
    }
}