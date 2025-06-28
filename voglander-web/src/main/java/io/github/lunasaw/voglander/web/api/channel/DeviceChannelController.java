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
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelUpdateReq;
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

    @GetMapping("/get/{id}")
    @Operation(summary = "根据ID获取设备通道", description = "通过设备通道ID获取设备通道详细信息")
    @ApiResponse(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult getById(@Parameter(description = "设备通道ID") @PathVariable(value = "id") Long id) {
        DeviceChannelDTO deviceChannelDTO = deviceChannelManager.getDeviceChannelDTOById(id);
        if (deviceChannelDTO == null) {
            return AjaxResult.error("设备通道不存在");
        }
        DeviceChannelVO deviceChannelVO = DeviceChannelVO.convertVO(deviceChannelDTO);
        return AjaxResult.success(deviceChannelVO);
    }

    @GetMapping("/get")
    @Operation(summary = "根据条件查询设备通道", description = "通过设备通道实体条件查询设备通道信息")
    public AjaxResult getByEntity(DeviceChannelDO deviceChannel) {
        DeviceChannelDTO deviceChannelDTO = deviceChannelManager.getDeviceChannelDTOByEntity(deviceChannel);
        if (deviceChannelDTO == null) {
            return AjaxResult.error("设备通道不存在");
        }
        DeviceChannelVO deviceChannelVO = DeviceChannelVO.convertVO(deviceChannelDTO);
        return AjaxResult.success(deviceChannelVO);
    }

    @GetMapping("/list")
    @Operation(summary = "获取设备通道列表", description = "根据条件获取设备通道列表")
    public AjaxResult list(DeviceChannelDO deviceChannel) {
        List<DeviceChannelDTO> deviceChannelDTOList = deviceChannelManager.listDeviceChannelDTO(deviceChannel);
        List<DeviceChannelVO> deviceChannelVOList = deviceChannelDTOList.stream()
                .map(DeviceChannelVO::convertVO)
                .collect(Collectors.toList());
        return AjaxResult.success(deviceChannelVOList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    @Operation(summary = "分页查询设备通道", description = "根据条件分页查询设备通道列表")
    public AjaxResult listPageByEntity(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size,
        DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = Wrappers.query(deviceChannel);
        Page<DeviceChannelDTO> pageInfo = deviceChannelManager.pageQuery(page, size, query);

        // 转换为 VO 模型
        List<DeviceChannelVO> deviceChannelVOList = pageInfo.getRecords().stream()
                .map(DeviceChannelVO::convertVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        Page<DeviceChannelVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(deviceChannelVOList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return AjaxResult.success(resultPage);
    }

    @GetMapping("/pageList/{page}/{size}")
    @Operation(summary = "简单分页查询", description = "分页查询所有设备通道")
    public AjaxResult listPage(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size) {
        Page<DeviceChannelDTO> pageInfo = deviceChannelManager.pageQuerySimple(page, size);

        // 转换为 VO 模型
        List<DeviceChannelVO> deviceChannelVOList = pageInfo.getRecords().stream()
                .map(DeviceChannelVO::convertVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        Page<DeviceChannelVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(deviceChannelVOList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return AjaxResult.success(resultPage);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建设备通道", description = "添加新的设备通道")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult insert(@RequestBody DeviceChannelCreateReq createReq) {
        // Req -> DTO (使用 Web 层转换器)
        DeviceChannelDTO deviceChannelDTO = deviceChannelWebAssembler.toDeviceChannelDTO(createReq);

        // 通过 Manager 层处理业务逻辑
        Long deviceChannelId = deviceChannelManager.createDeviceChannel(deviceChannelDTO);

        return AjaxResult.success(deviceChannelId);
    }

    @PostMapping("/insertBatch")
    @Operation(summary = "批量创建设备通道", description = "批量添加设备通道")
    public AjaxResult insertBatch(@RequestBody List<DeviceChannelCreateReq> createReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<DeviceChannelDTO> deviceChannelDTOList = deviceChannelWebAssembler.toDeviceChannelDTOList(createReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = deviceChannelManager.batchCreateDeviceChannel(deviceChannelDTOList);

        return AjaxResult.success("成功创建 " + successCount + " 个设备通道，共 " + createReqList.size() + " 个请求");
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备通道", description = "更新设备通道信息")
    public AjaxResult update(@RequestBody DeviceChannelUpdateReq updateReq) {
        // Req -> DTO (使用 Web 层转换器)
        DeviceChannelDTO deviceChannelDTO = deviceChannelWebAssembler.toDeviceChannelDTO(updateReq);

        Long updated = deviceChannelManager.updateDeviceChannel(deviceChannelDTO);

        return AjaxResult.success(updated);
    }

    @PutMapping("/updateBatch")
    @Operation(summary = "批量更新设备通道", description = "批量更新设备通道信息")
    public AjaxResult updateBatch(@RequestBody List<DeviceChannelUpdateReq> updateReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<DeviceChannelDTO> deviceChannelDTOList = deviceChannelWebAssembler.toUpdateDeviceChannelDTOList(updateReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = deviceChannelManager.batchUpdateDeviceChannel(deviceChannelDTOList);

        return AjaxResult.success("成功更新 " + successCount + " 个设备通道，共 " + updateReqList.size() + " 个请求");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除设备通道", description = "根据ID删除设备通道")
    public AjaxResult deleteOne(@Parameter(description = "设备通道ID") @PathVariable(value = "id") Long id) {
        return AjaxResult.success(deviceChannelService.removeById(id));
    }

    @DeleteMapping("/deleteByEntity")
    @Operation(summary = "按条件删除设备通道", description = "根据设备通道实体条件删除设备通道")
    public AjaxResult deleteOne(@RequestBody DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = Wrappers.query(deviceChannel);
        return AjaxResult.success(deviceChannelService.remove(query));
    }

    @DeleteMapping("/deleteIds")
    @Operation(summary = "批量删除设备通道", description = "根据ID列表批量删除设备通道")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        return AjaxResult.success(deviceChannelService.removeBatchByIds(ids));
    }

    @GetMapping("/count")
    @Operation(summary = "统计设备通道总数", description = "获取设备通道总数量")
    public AjaxResult getAccount() {
        return AjaxResult.success(deviceChannelService.count());
    }

    @GetMapping("/countByEntity")
    @Operation(summary = "按条件统计设备通道", description = "根据条件统计设备通道数量")
    public AjaxResult getAccountByEntity(DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = Wrappers.query(deviceChannel);
        return AjaxResult.success(deviceChannelService.count(query));
    }
}
