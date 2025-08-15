package io.github.lunasaw.voglander.web.api.channel;

import java.util.List;
import java.util.stream.Collectors;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.web.api.channel.assembler.DeviceChannelWebAssembler;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelCreateReq;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelQueryReq;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelUpdateReq;
import io.github.lunasaw.voglander.web.api.channel.resp.DeviceChannelListResp;
import io.github.lunasaw.voglander.web.api.channel.vo.DeviceChannelVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.domain.AjaxResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 设备通道管理
 *
 * @Author: chenzhangyue
 * @CreateTime: 2024-01-30 14:20:37
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/deviceChannel")
@Tag(name = "设备通道管理", description = "设备通道管理相关接口")
public class DeviceChannelController {

    @Autowired
    private DeviceChannelService deviceChannelService;

    @Autowired
    private DeviceChannelManager deviceChannelManager;

    @Autowired
    private DeviceChannelWebAssembler deviceChannelWebAssembler;

    // ================================
    // 核心模板方法（必须实现）
    // ================================

    @PostMapping("/add")
    @Operation(summary = "新增数据", description = "标准数据创建，校验参数并插入数据库")
    public AjaxResult<Long> add(@Valid @RequestBody DeviceChannelCreateReq createReq) {
        DeviceChannelDTO dto = deviceChannelWebAssembler.createReqToDto(createReq);
        Long id = deviceChannelManager.add(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新数据", description = "通过主键ID更新指定字段，要求必须携带ID")
    public AjaxResult<Long> update(@Valid @RequestBody DeviceChannelUpdateReq updateReq) {
        DeviceChannelDTO updateDTO = deviceChannelWebAssembler.updateReqToDto(updateReq);
        Long id = deviceChannelManager.updateById(updateReq.getId(), updateDTO);
        return AjaxResult.success(id);
    }

    @PostMapping("/get")
    @Operation(summary = "灵活单条查询", description = "支持多种条件查询")
    public AjaxResult<DeviceChannelVO> get(@RequestBody DeviceChannelQueryReq queryReq) {
        DeviceChannelDTO queryDTO = deviceChannelWebAssembler.queryReqToDto(queryReq);
        DeviceChannelDTO result = deviceChannelManager.get(queryDTO);
        if (result == null) {
            return AjaxResult.error("记录不存在");
        }
        DeviceChannelVO vo = deviceChannelWebAssembler.dtoToVo(result);
        return AjaxResult.success(vo);
    }

    @DeleteMapping("/deleteOne")
    @Operation(summary = "单条记录删除", description = "支持多种删除策略")
    public AjaxResult<Void> deleteOne(@RequestBody DeviceChannelUpdateReq deleteReq) {
        DeviceChannelDTO deleteDTO = deviceChannelWebAssembler.updateReqToDto(deleteReq);
        Boolean success = deviceChannelManager.deleteOne(deleteDTO);
        if (success) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    @DeleteMapping("/deleteBatch")
    @Operation(summary = "批量记录删除", description = "支持多种条件的批量删除")
    public AjaxResult<Void> deleteBatch(@RequestBody DeviceChannelQueryReq queryReq) {
        DeviceChannelDTO queryDTO = deviceChannelWebAssembler.queryReqToDto(queryReq);
        Boolean success = deviceChannelManager.deleteBatch(queryDTO);
        if (success) {
            return AjaxResult.success("批量删除成功");
        } else {
            return AjaxResult.error("批量删除失败");
        }
    }

    @PostMapping("/getPage")
    @Operation(summary = "分页条件查询", description = "全量分页条件搜索")
    public AjaxResult<DeviceChannelListResp> getPage(
        @RequestBody(required = false) DeviceChannelQueryReq queryReq,
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "页大小") @RequestParam(defaultValue = "10") int size) {

        DeviceChannelDTO queryDTO = deviceChannelWebAssembler.queryReqToDto(queryReq);
        Page<DeviceChannelDTO> pageResult = deviceChannelManager.getPage(queryDTO, page, size);

        List<DeviceChannelVO> voList = pageResult.getRecords().stream()
            .map(deviceChannelWebAssembler::dtoToVo)
            .collect(Collectors.toList());

        DeviceChannelListResp resp = new DeviceChannelListResp();
        resp.setTotal(pageResult.getTotal());
        resp.setItems(voList);
        return AjaxResult.success(resp);
    }

    // ================================
    // 增强业务方法（可选实现）
    // ================================

    @DeleteMapping("/deleteDeviceChannel")
    @Operation(summary = "业务删除", description = "业务删除，包含操作日志记录")
    public AjaxResult<Boolean> deleteDeviceChannel(@RequestBody DeviceChannelUpdateReq deviceChannelUpdateReq) {
        DeviceChannelDTO deviceChannelDTO = deviceChannelWebAssembler.updateReqToDto(deviceChannelUpdateReq);
        Boolean success = deviceChannelManager.deleteDeviceChannel(deviceChannelDTO.getDeviceId(), deviceChannelDTO.getChannelId());
        return AjaxResult.success(success);
    }

    @PostMapping("/createDeviceChannel")
    @Operation(summary = "业务创建", description = "业务创建，包含完整的业务逻辑")
    public AjaxResult<Long> createDeviceChannel(@Valid @RequestBody DeviceChannelCreateReq createReq) {
        DeviceChannelDTO dto = deviceChannelWebAssembler.createReqToDto(createReq);
        Long id = deviceChannelManager.createDeviceChannel(dto);
        return AjaxResult.success(id);
    }

    @PutMapping("/updateDeviceChannel")
    @Operation(summary = "业务更新", description = "业务更新，包含完整的业务逻辑")
    public AjaxResult<Long> updateDeviceChannel(@Valid @RequestBody DeviceChannelUpdateReq updateReq) {
        DeviceChannelDTO dto = deviceChannelWebAssembler.updateReqToDto(updateReq);
        Long id = deviceChannelManager.updateDeviceChannel(dto);
        return AjaxResult.success(id);
    }

    // ================================
    // 兼容历史接口（保持向后兼容）
    // ================================

    @GetMapping("/get/{id}")
    @Operation(summary = "根据ID获取设备通道", description = "通过设备通道ID获取设备通道详细信息")
    @ApiResponse(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult<DeviceChannelVO> getById(@Parameter(description = "设备通道ID") @PathVariable(value = "id") Long id) {
        DeviceChannelQueryReq queryReq = new DeviceChannelQueryReq();
        queryReq.setId(id);
        return get(queryReq);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建设备通道", description = "添加新的设备通道")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult<Long> insert(@Valid @RequestBody DeviceChannelCreateReq createReq) {
        return add(createReq);
    }

    @PostMapping("/insertBatch")
    @Operation(summary = "批量创建设备通道", description = "批量添加设备通道")
    public AjaxResult<String> insertBatch(@Valid @RequestBody List<DeviceChannelCreateReq> createReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<DeviceChannelDTO> deviceChannelDTOList = deviceChannelWebAssembler.toDeviceChannelDTOList(createReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = deviceChannelManager.batchCreateDeviceChannel(deviceChannelDTOList);

        String message = "成功创建 " + successCount + " 个设备通道，共 " + createReqList.size() + " 个请求";
        return AjaxResult.success("操作成功", message);
    }

    @GetMapping("/pageList/{page}/{size}")
    @Operation(summary = "简单分页查询", description = "分页查询所有设备通道")
    public AjaxResult<DeviceChannelListResp> listPage(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size) {
        return getPage(null, page, size);
    }
}
