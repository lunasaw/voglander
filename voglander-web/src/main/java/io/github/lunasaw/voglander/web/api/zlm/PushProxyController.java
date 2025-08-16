package io.github.lunasaw.voglander.web.api.zlm;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luna.common.check.Assert;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.manager.manager.PushProxyManager;
import io.github.lunasaw.voglander.service.stream.PushProxyBizService;
import io.github.lunasaw.voglander.web.api.zlm.assembler.PushProxyWebAssembler;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyQueryReq;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.PushProxyListResp;
import io.github.lunasaw.voglander.web.api.zlm.vo.PushProxyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 推流代理管理
 *
 * @author luna
 * @since 2025-01-23
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/push-proxy")
@Tag(name = "推流代理管理", description = "推流代理增删改查等相关接口")
public class PushProxyController {

    @Autowired
    private PushProxyManager      pushProxyManager;

    @Autowired
    private PushProxyWebAssembler pushProxyWebAssembler;

    @Autowired
    private PushProxyBizService   pushProxyBizService;

    @GetMapping("/get/{id}")
    @Operation(summary = "根据ID获取推流代理", description = "通过数据库主键ID获取推流代理详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult<PushProxyVO> getById(@Parameter(description = "推流代理数据库ID") @PathVariable(value = "id") Long id) {
        PushProxyDTO pushProxyDTO = pushProxyManager.getById(id);
        if (pushProxyDTO == null) {
            return AjaxResult.error("推流代理不存在");
        }

        // 直接转换DTO为VO
        PushProxyVO vo = pushProxyWebAssembler.dtoToVo(pushProxyDTO);
        return AjaxResult.success(vo);
    }

    // ================================
    // 核心模板方法实现
    // ================================

    @PostMapping("/add")
    @Operation(summary = "新增推流代理", description = "标准数据创建，校验参数并插入数据库")
    @ApiResponse(responseCode = "200", description = "创建成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult<Long> add(@Valid @RequestBody PushProxyCreateReq createReq) {
        // 转换请求为DTO
        PushProxyDTO dto = pushProxyWebAssembler.createReqToDto(createReq);

        Long id = pushProxyManager.add(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新推流代理", description = "通过主键ID更新指定字段，要求必须携带ID")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Long> updateProxy(@Valid @RequestBody PushProxyUpdateReq updateReq) {
        // 转换请求为DTO
        PushProxyDTO dto = pushProxyWebAssembler.updateReqToDto(updateReq);

        Long id = pushProxyManager.updateById(updateReq.getId(), dto);
        return AjaxResult.success(id);
    }

    @PostMapping("/get")
    @Operation(summary = "灵活单条查询", description = "支持ID、app+stream、proxyKey等多种条件查询")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public AjaxResult<PushProxyVO> getProxy(@RequestBody PushProxyQueryReq queryReq) {
        PushProxyDTO queryDTO = pushProxyWebAssembler.queryReqToDto(queryReq);
        PushProxyDTO result = pushProxyManager.get(queryDTO);
        if (result == null) {
            return AjaxResult.error("推流代理不存在");
        }

        PushProxyVO vo = pushProxyWebAssembler.dtoToVo(result);
        return AjaxResult.success(vo);
    }

    @DeleteMapping("/deleteOne")
    @Operation(summary = "单条记录删除", description = "支持ID、proxyKey、app+stream优先级删除策略")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult<Void> deleteOne(@RequestBody PushProxyUpdateReq deleteReq) {
        PushProxyDTO deleteDTO = pushProxyWebAssembler.updateReqToDto(deleteReq);
        Assert.notNull(deleteDTO, "删除条件不能为空");
        boolean success = pushProxyBizService.deletePushProxyWithTermination(deleteDTO);
        if (success) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    @DeleteMapping("/deleteBatch")
    @Operation(summary = "批量删除", description = "支持多种条件组合的批量删除")
    @ApiResponse(responseCode = "200", description = "批量删除成功")
    public AjaxResult<Void> deleteBatch(@RequestBody PushProxyUpdateReq deleteReq) {
        PushProxyDTO deleteDTO = pushProxyWebAssembler.updateReqToDto(deleteReq);
        Boolean success = pushProxyManager.deleteBatch(deleteDTO);
        if (success) {
            return AjaxResult.success("批量删除成功");
        } else {
            return AjaxResult.error("批量删除失败");
        }
    }

    @PostMapping("/getPage")
    @Operation(summary = "分页条件查询", description = "全量分页条件搜索，支持复杂条件查询")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public AjaxResult<PushProxyListResp> getPageWithConditions(
        @RequestBody(required = false) PushProxyQueryReq queryReq,
        @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "页大小，默认10") @RequestParam(defaultValue = "10") int size) {

        PushProxyDTO queryDTO = pushProxyWebAssembler.queryReqToDto(queryReq);
        Page<PushProxyDTO> pageResult = pushProxyManager.getPage(queryDTO, page, size);

        List<PushProxyVO> voList = pageResult.getRecords().stream()
            .map(pushProxyWebAssembler::dtoToVo)
            .collect(Collectors.toList());

        PushProxyListResp resp = new PushProxyListResp();
        resp.setTotal(pageResult.getTotal());
        resp.setItems(voList);

        return AjaxResult.success(resp);
    }

    // ================================
    // 增强业务方法（包含操作日志）
    // ================================

    @PostMapping("/createPushProxy")
    @Operation(summary = "业务创建推流代理", description = "业务创建，设置默认值并记录操作日志")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult<Long> createPushProxy(@Valid @RequestBody PushProxyCreateReq createReq) {
        PushProxyDTO dto = pushProxyWebAssembler.createReqToDto(createReq);

        Long id = pushProxyManager.createPushProxy(dto);
        return AjaxResult.success(id);
    }

    @PostMapping("/createPushProxyWithNode")
    @Operation(summary = "业务创建推流代理（指定节点）", description = "在指定节点创建推流代理并启动")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult<Long> createPushProxyWithNode(@Valid @RequestBody PushProxyCreateReq createReq) {
        PushProxyDTO dto = pushProxyWebAssembler.createReqToDto(createReq);

        Long id = pushProxyBizService.createPushProxyWithSpecificNode(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/updatePushProxy")
    @Operation(summary = "业务更新推流代理", description = "业务更新，包含操作日志记录")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Boolean> updatePushProxy(
        @Valid @RequestBody PushProxyUpdateReq updateReq,
        @Parameter(description = "操作描述") @RequestParam(defaultValue = "更新推流代理") String operationDesc) {

        PushProxyDTO dto = pushProxyWebAssembler.updateReqToDto(updateReq);

        Boolean success = pushProxyManager.updatePushProxy(dto, operationDesc);
        return AjaxResult.success(success);
    }

    @DeleteMapping("/deletePushProxy")
    @Operation(summary = "业务删除推流代理", description = "业务删除，包含操作日志记录")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public AjaxResult<Boolean> deletePushProxy(@RequestBody PushProxyUpdateReq pushProxyUpdateReq) {
        PushProxyDTO pushProxyDTO = pushProxyWebAssembler.updateReqToDto(pushProxyUpdateReq);
        Boolean success = pushProxyManager.deletePushProxy(pushProxyDTO, "删除推流代理");
        return AjaxResult.success(success);
    }

    @PutMapping("/updateStatus/{id}")
    @Operation(summary = "更新推流代理状态", description = "根据ID启用/禁用推流代理，status：1启用 0禁用")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public AjaxResult<Boolean> updatePushProxyStatus(
        @Parameter(description = "推流代理数据库ID") @PathVariable("id") Long id,
        @Parameter(description = "状态：1启用 0禁用") @RequestParam("status") Integer status) {
        boolean success = pushProxyBizService.updatePushProxyStatus(id, status);
        return AjaxResult.success(success);
    }

    @PostMapping("/start/{id}")
    @Operation(summary = "启动推流代理", description = "启动指定ID的推流代理")
    @ApiResponse(responseCode = "200", description = "启动成功")
    public AjaxResult<Boolean> startPushProxy(
        @Parameter(description = "推流代理数据库ID") @PathVariable("id") Long id) {
        boolean success = pushProxyBizService.startPushProxy(id);
        return AjaxResult.success(success);
    }

    @PostMapping("/stop/{id}")
    @Operation(summary = "停止推流代理", description = "停止指定ID的推流代理")
    @ApiResponse(responseCode = "200", description = "停止成功")
    public AjaxResult<Boolean> stopPushProxy(
        @Parameter(description = "推流代理数据库ID") @PathVariable("id") Long id) {
        boolean success = pushProxyBizService.stopPushProxy(id);
        return AjaxResult.success(success);
    }

    @GetMapping("/checkSource")
    @Operation(summary = "检查源流是否在线", description = "检查指定应用和流的源流是否在线")
    @ApiResponse(responseCode = "200", description = "检查完成")
    public AjaxResult<Boolean> checkSourceStreamOnline(
        @Parameter(description = "节点ID") @RequestParam("serverId") String serverId,
        @Parameter(description = "应用名称") @RequestParam("app") String app,
        @Parameter(description = "流名称") @RequestParam("stream") String stream) {
        boolean isOnline = pushProxyBizService.checkSourceStreamOnline(serverId, app, stream);
        return AjaxResult.success(isOnline);
    }
}