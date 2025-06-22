package io.github.lunasaw.voglander.web.api.device;

import java.util.List;
import java.util.stream.Collectors;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.web.api.device.assembler.DeviceWebAssembler;
import io.github.lunasaw.voglander.web.api.device.req.DeviceCreateReq;
import io.github.lunasaw.voglander.web.api.device.req.DeviceUpdateReq;
import io.github.lunasaw.voglander.web.api.device.vo.DeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;

/**
 * @Author: chenzhangyue
 * @CreateTime: 2024-01-30 14:19:15
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/device")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private DeviceWebAssembler deviceWebAssembler;

    @GetMapping("/get/{id}")
    public AjaxResult getById(@PathVariable(value = "id") Long id) {
        DeviceDTO deviceDTO = deviceManager.getDeviceDTOById(id);
        if (deviceDTO == null) {
            return AjaxResult.error("设备不存在");
        }
        DeviceVO deviceVO = DeviceVO.convertVO(deviceDTO);
        return AjaxResult.success(deviceVO);
    }

    @GetMapping("/get")
    public AjaxResult getByEntity(DeviceDO device) {
        DeviceDTO deviceDTO = deviceManager.getDeviceDTOByEntity(device);
        if (deviceDTO == null) {
            return AjaxResult.error("设备不存在");
        }
        DeviceVO deviceVO = DeviceVO.convertVO(deviceDTO);
        return AjaxResult.success(deviceVO);
    }

    @GetMapping("/list")
    public AjaxResult list(DeviceDO device) {
        List<DeviceDTO> deviceDTOList = deviceManager.listDeviceDTO(device);
        List<DeviceVO> deviceVOList = deviceDTOList.stream()
                .map(DeviceVO::convertVO)
                .collect(Collectors.toList());
        return AjaxResult.success(deviceVOList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    public AjaxResult listPageByEntity(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size, DeviceDO device) {
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
    public AjaxResult listPage(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size) {
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
    public AjaxResult insert(@RequestBody DeviceCreateReq createReq) {
        try {
            // Req -> DTO (使用 Web 层转换器)
            DeviceDTO deviceDTO = deviceWebAssembler.toDeviceDTO(createReq);

            // 通过 Manager 层处理业务逻辑
            Long deviceId = deviceManager.createDevice(deviceDTO);

            return AjaxResult.success(deviceId);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("创建设备失败");
        }
    }



    @PostMapping("/insertBatch")
    public AjaxResult insertBatch(@RequestBody List<DeviceCreateReq> createReqList) {
        try {
            // 批量 Req -> DTO (使用 Web 层转换器)
            List<DeviceDTO> deviceDTOList = deviceWebAssembler.toDeviceDTOList(createReqList);

            // 通过 Manager 层处理批量业务逻辑
            int successCount = deviceManager.batchCreateDevice(deviceDTOList);

            return AjaxResult.success("成功创建 " + successCount + " 个设备，共 " + createReqList.size() + " 个请求");
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("批量创建设备失败");
        }
    }

    @PutMapping("/update")
    public AjaxResult update(@RequestBody DeviceUpdateReq updateReq) {
        try {
            // Req -> DTO (使用 Web 层转换器)
            DeviceDTO deviceDTO = deviceWebAssembler.toDeviceDTO(updateReq);

            Long updated = deviceManager.updateDevice(deviceDTO);

            return AjaxResult.success(updated);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("更新设备失败");
        }
    }

    @PutMapping("/updateBatch")
    public AjaxResult updateBatch(@RequestBody List<DeviceUpdateReq> updateReqList) {
        try {
            // 批量 Req -> DTO (使用 Web 层转换器)
            List<DeviceDTO> deviceDTOList = deviceWebAssembler.toUpdateDeviceDTOList(updateReqList);

            // 通过 Manager 层处理批量业务逻辑
            int successCount = deviceManager.batchUpdateDevice(deviceDTOList);

            return AjaxResult.success("成功更新 " + successCount + " 个设备，共 " + updateReqList.size() + " 个请求");
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            return AjaxResult.error("批量更新设备失败");
        }
    }

    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteOne(@PathVariable(value = "id") Long id) {
        return AjaxResult.success(deviceService.removeById(id));
    }

    @DeleteMapping("/deleteIds")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        return AjaxResult.success(deviceService.removeBatchByIds(ids));
    }

    @GetMapping("/count")
    public AjaxResult getAccount() {
        return AjaxResult.success(deviceService.count());
    }

    @GetMapping("/countByEntity")
    public AjaxResult getAccountByEntity(DeviceDO device) {
        QueryWrapper<DeviceDO> query = Wrappers.query(device);
        return AjaxResult.success(deviceService.count(query));
    }
}
