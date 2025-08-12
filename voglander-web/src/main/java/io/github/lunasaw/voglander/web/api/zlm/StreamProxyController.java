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
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyQueryReq;
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

    // ================================
    // 管理器层模版方法实现
    // ================================

    @PostMapping("/add")
    @Operation(summary = "新增拉流代理", description = "标准数据创建，校验参数并插入数据库")
    @ApiResponse(responseCode = "200", description = "创建成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult<Long> add(@Valid @RequestBody StreamProxyCreateReq createReq) {
        // 转换请求为DTO
        StreamProxyDTO dto = streamProxyWebAssembler.createReqToDto(createReq);

        Long id = streamProxyManager.add(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新拉流代理", description = "智能更新，优先使用ID，否则使用业务键")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Long> updateProxy(@Valid @RequestBody StreamProxyUpdateReq updateReq) {
        // 转换请求为DTO
        StreamProxyDTO dto = streamProxyWebAssembler.updateReqToDto(updateReq);

        Long id = streamProxyManager.updateById(updateReq.getId(), dto);
        return AjaxResult.success(id);
    }

    @PostMapping("/get")
    @Operation(summary = "灵活单条查询", description = "支持ID、app+stream、proxyKey等多种条件查询")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public AjaxResult<StreamProxyVO> getProxy(@RequestBody StreamProxyQueryReq queryReq) {
        StreamProxyDTO queryDTO = streamProxyWebAssembler.queryReqToDto(queryReq);
        StreamProxyDTO result = streamProxyManager.get(queryDTO);
        if (result == null) {
            return AjaxResult.error("代理不存在");
        }

        StreamProxyVO vo = streamProxyWebAssembler.dtoToVo(result);
        return AjaxResult.success(vo);
    }

    @DeleteMapping("/deleteOne")
    @Operation(summary = "单条记录删除", description = "支持ID、proxyKey、app+stream优先级删除策略")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult<Void> deleteOne(@RequestBody StreamProxyUpdateReq deleteReq) {
        StreamProxyDTO deleteDTO = streamProxyWebAssembler.updateReqToDto(deleteReq);
        Boolean success = streamProxyManager.deleteOne(deleteDTO);
        if (success) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    @DeleteMapping("/deleteBatch")
    @Operation(summary = "批量删除", description = "支持多种条件组合的批量删除")
    @ApiResponse(responseCode = "200", description = "批量删除成功")
    public AjaxResult<Void> deleteBatch(@RequestBody StreamProxyUpdateReq deleteReq) {
        StreamProxyDTO deleteDTO = streamProxyWebAssembler.updateReqToDto(deleteReq);
        Boolean success = streamProxyManager.deleteBatch(deleteDTO);
        if (success) {
            return AjaxResult.success("批量删除成功");
        } else {
            return AjaxResult.error("批量删除失败");
        }
    }

    @PostMapping("/getPage")
    @Operation(summary = "分页条件查询", description = "全量分页条件搜索，支持复杂条件查询")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public AjaxResult<StreamProxyListResp> getPageWithConditions(
        @RequestBody(required = false) StreamProxyQueryReq queryReq,
        @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "页大小，默认10") @RequestParam(defaultValue = "10") int size) {

        StreamProxyDTO queryDTO = streamProxyWebAssembler.queryReqToDto(queryReq);
        Page<StreamProxyDTO> pageResult = streamProxyManager.getPage(queryDTO, page, size);

        List<StreamProxyVO> voList = pageResult.getRecords().stream()
            .map(streamProxyWebAssembler::dtoToVo)
            .collect(Collectors.toList());

        StreamProxyListResp resp = new StreamProxyListResp();
        resp.setTotal(pageResult.getTotal());
        resp.setItems(voList);

        return AjaxResult.success(resp);
    }

    // ================================
    // 增强业务方法（包含操作日志）
    // ================================

    @PostMapping("/createStreamProxy")
    @Operation(summary = "业务创建代理", description = "业务创建，设置默认值并记录操作日志")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult<Long> createStreamProxy(@Valid @RequestBody StreamProxyCreateReq createReq) {
        StreamProxyDTO dto = streamProxyWebAssembler.createReqToDto(createReq);

        Long id = streamProxyManager.createStreamProxy(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/updateStreamProxy")
    @Operation(summary = "业务更新代理", description = "业务更新，包含操作日志记录")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Boolean> updateStreamProxy(
        @Valid @RequestBody StreamProxyUpdateReq updateReq,
        @Parameter(description = "操作描述") @RequestParam(defaultValue = "更新拉流代理") String operationDesc) {

        StreamProxyDTO dto = streamProxyWebAssembler.updateReqToDto(updateReq);

        Boolean success = streamProxyManager.updateStreamProxy(dto, operationDesc);
        return AjaxResult.success(success);
    }

    @DeleteMapping("/deleteStreamProxy")
    @Operation(summary = "业务删除代理", description = "业务删除，包含操作日志记录")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult<Boolean> deleteStreamProxy(@RequestBody StreamProxyUpdateReq streamProxyUpdateReq) {
        StreamProxyDTO streamProxyDTO = streamProxyWebAssembler.updateReqToDto(streamProxyUpdateReq);
        Boolean success = streamProxyManager.deleteStreamProxy(streamProxyDTO, "删除拉流代理");
        return AjaxResult.success(success);
    }
}