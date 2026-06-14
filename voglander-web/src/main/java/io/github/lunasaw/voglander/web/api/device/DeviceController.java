package io.github.lunasaw.voglander.web.api.device;

import java.util.List;
import java.util.stream.Collectors;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceSubscriptionDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.DeviceSubscriptionManager;
import io.github.lunasaw.voglander.web.api.device.assembler.DeviceWebAssembler;
import io.github.lunasaw.voglander.web.api.device.req.DeviceCreateReq;
import io.github.lunasaw.voglander.web.api.device.req.DevicePageReq;
import io.github.lunasaw.voglander.web.api.device.req.DeviceUpdateReq;
import io.github.lunasaw.voglander.web.api.device.resp.DeviceListResp;
import io.github.lunasaw.voglander.web.api.device.vo.DeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 设备管理
 *
 * @Author: chenzhangyue
 * @CreateTime: 2024-01-30 14:19:15
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/device")
@Tag(name = "设备管理", description = "设备管理相关接口")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private DeviceChannelManager deviceChannelManager;

    @Autowired
    private DeviceSubscriptionManager deviceSubscriptionManager;

    @Autowired
    private DeviceWebAssembler deviceWebAssembler;

    @PostMapping("/getPage")
    @Operation(summary = "分页条件查询", description = "全量分页条件搜索，前端灵活组装条件（deviceId/名称/状态/类型/IP/时间范围）")
    @ApiResponse(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult getPage(
        @RequestBody(required = false) DevicePageReq pageReq,
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        DeviceQueryDTO query = deviceWebAssembler.pageReqToQueryDto(pageReq);
        Page<DeviceDTO> dtoPage = deviceManager.getPage(query, page, size);

        List<DeviceVO> items = dtoPage.getRecords().stream()
            .map(DeviceVO::convertVO)
            .peek(vo -> vo.setChannelCount((int) deviceChannelManager.countByDeviceId(vo.getDeviceId())))
            .collect(Collectors.toList());

        // 批量回填三类订阅意图状态（避免 N+1）
        fillSubscriptionState(items);

        DeviceListResp resp = new DeviceListResp();
        resp.setTotal(dtoPage.getTotal());
        resp.setItems(items);
        return AjaxResult.success(resp);
    }

    /**
     * 批量按 deviceId 查 tb_device_subscription，填充每个设备 VO 的订阅意图开关状态。
     */
    private void fillSubscriptionState(List<DeviceVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<String> deviceIds = items.stream().map(DeviceVO::getDeviceId).collect(Collectors.toList());
        List<DeviceSubscriptionDTO> subs = deviceSubscriptionManager.listByDeviceIds(deviceIds);
        // deviceId → SubscriptionVO（按 sub_type + enabled 聚合）
        java.util.Map<String, DeviceVO.SubscriptionVO> byDevice = new java.util.HashMap<>();
        for (DeviceSubscriptionDTO sub : subs) {
            DeviceVO.SubscriptionVO vo = byDevice.computeIfAbsent(sub.getDeviceId(), k -> new DeviceVO.SubscriptionVO());
            boolean on = sub.getEnabled() != null && sub.getEnabled() == 1;
            if (SubscriptionConstant.Type.CATALOG.name().equals(sub.getSubType())) {
                vo.setCatalog(on);
            } else if (SubscriptionConstant.Type.MOBILE_POSITION.name().equals(sub.getSubType())) {
                vo.setPosition(on);
            } else if (SubscriptionConstant.Type.ALARM.name().equals(sub.getSubType())) {
                vo.setAlarm(on);
            }
        }
        for (DeviceVO vo : items) {
            vo.setSubscription(byDevice.getOrDefault(vo.getDeviceId(), new DeviceVO.SubscriptionVO()));
        }
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "根据ID获取设备", description = "通过设备ID获取设备详细信息")
    @ApiResponse(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = AjaxResult.class)))
    public AjaxResult getById(@Parameter(description = "设备ID") @PathVariable(value = "id") Long id) {
        DeviceDTO deviceDTO = deviceManager.getDeviceDTOById(id);
        if (deviceDTO == null) {
            return AjaxResult.error("设备不存在");
        }
        DeviceVO deviceVO = DeviceVO.convertVO(deviceDTO);
        return AjaxResult.success(deviceVO);
    }

    @GetMapping("/get")
    @Operation(summary = "根据条件查询设备", description = "通过设备实体条件查询设备信息")
    public AjaxResult getByEntity(DeviceDO device) {
        DeviceDTO deviceDTO = deviceManager.getDeviceDTOByEntity(device);
        if (deviceDTO == null) {
            return AjaxResult.error("设备不存在");
        }
        DeviceVO deviceVO = DeviceVO.convertVO(deviceDTO);
        return AjaxResult.success(deviceVO);
    }

    @GetMapping("/list")
    @Operation(summary = "获取设备列表", description = "根据条件获取设备列表")
    public AjaxResult list(DeviceDO device) {
        List<DeviceDTO> deviceDTOList = deviceManager.listDeviceDTO(device);
        List<DeviceVO> deviceVOList = deviceDTOList.stream()
                .map(DeviceVO::convertVO)
                .collect(Collectors.toList());
        return AjaxResult.success(deviceVOList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    @Operation(summary = "分页查询设备", description = "根据条件分页查询设备列表")
    public AjaxResult listPageByEntity(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size,
        DeviceDO device) {
        QueryWrapper<DeviceDO> query = Wrappers.query(device);
        Page<DeviceDTO> pageInfo = deviceManager.pageQuery(page, size, query);

        // 转换为 VO 模型
        List<DeviceVO> deviceVOList = pageInfo.getRecords().stream()
                .map(DeviceVO::convertVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        Page<DeviceVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(deviceVOList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return AjaxResult.success(resultPage);
    }

    @GetMapping("/pageList/{page}/{size}")
    @Operation(summary = "简单分页查询", description = "分页查询所有设备")
    public AjaxResult listPage(
        @Parameter(description = "页码") @PathVariable(value = "page") int page,
        @Parameter(description = "每页大小") @PathVariable(value = "size") int size) {
        Page<DeviceDTO> pageInfo = deviceManager.pageQuerySimple(page, size);

        // 转换为 VO 模型
        List<DeviceVO> deviceVOList = pageInfo.getRecords().stream()
                .map(DeviceVO::convertVO)
                .collect(Collectors.toList());

        // 构建返回的分页对象
        Page<DeviceVO> resultPage = new Page<>(page, size);
        resultPage.setRecords(deviceVOList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return AjaxResult.success(resultPage);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建设备", description = "添加新的设备")
    @ApiResponse(responseCode = "200", description = "创建成功")
    public AjaxResult insert(@RequestBody DeviceCreateReq createReq) {
        // Req -> DTO (使用 Web 层转换器)
        DeviceDTO deviceDTO = deviceWebAssembler.toDeviceDTO(createReq);

        // 通过 Manager 层处理业务逻辑
        Long deviceId = deviceManager.createDevice(deviceDTO);

        return AjaxResult.success(deviceId);
    }

    @PostMapping("/insertBatch")
    @Operation(summary = "批量创建设备", description = "批量添加设备")
    public AjaxResult insertBatch(@RequestBody List<DeviceCreateReq> createReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<DeviceDTO> deviceDTOList = deviceWebAssembler.toDeviceDTOList(createReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = deviceManager.batchCreateDevice(deviceDTOList);

        return AjaxResult.success("成功创建 " + successCount + " 个设备，共 " + createReqList.size() + " 个请求");
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备", description = "更新设备信息")
    public AjaxResult update(@RequestBody DeviceUpdateReq updateReq) {
        // Req -> DTO (使用 Web 层转换器)
        DeviceDTO deviceDTO = deviceWebAssembler.toDeviceDTO(updateReq);

        Long updated = deviceManager.updateDevice(deviceDTO);

        return AjaxResult.success(updated);
    }

    @PutMapping("/updateBatch")
    @Operation(summary = "批量更新设备", description = "批量更新设备信息")
    public AjaxResult updateBatch(@RequestBody List<DeviceUpdateReq> updateReqList) {
        // 批量 Req -> DTO (使用 Web 层转换器)
        List<DeviceDTO> deviceDTOList = deviceWebAssembler.toUpdateDeviceDTOList(updateReqList);

        // 通过 Manager 层处理批量业务逻辑
        int successCount = deviceManager.batchUpdateDevice(deviceDTOList);

        return AjaxResult.success("成功更新 " + successCount + " 个设备，共 " + updateReqList.size() + " 个请求");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除设备", description = "根据ID删除设备")
    public AjaxResult deleteOne(@Parameter(description = "设备ID") @PathVariable(value = "id") Long id) {
        return AjaxResult.success(deviceService.removeById(id));
    }

    @DeleteMapping("/deleteIds")
    @Operation(summary = "批量删除设备", description = "根据ID列表批量删除设备")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        return AjaxResult.success(deviceService.removeBatchByIds(ids));
    }

    @GetMapping("/count")
    @Operation(summary = "统计设备总数", description = "获取设备总数量")
    public AjaxResult getAccount() {
        return AjaxResult.success(deviceService.count());
    }

    @GetMapping("/countByEntity")
    @Operation(summary = "按条件统计设备", description = "根据条件统计设备数量")
    public AjaxResult getAccountByEntity(DeviceDO device) {
        QueryWrapper<DeviceDO> query = Wrappers.query(device);
        return AjaxResult.success(deviceService.count(query));
    }
}