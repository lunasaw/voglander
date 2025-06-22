package io.github.lunasaw.voglander.web.api.device;

import java.util.List;
import java.util.stream.Collectors;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.web.api.device.vo.DeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
    private DeviceAssembler deviceAssembler;

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
    public AjaxResult insert(@RequestBody DeviceDO device) {
        deviceService.save(device);
        return AjaxResult.success(device);
    }

    @PostMapping("/insertBatch")
    public AjaxResult insert(@RequestBody List<DeviceDO> list) {
        boolean saved = deviceService.saveBatch(list);
        return AjaxResult.success(saved);
    }

    @PutMapping("/update")
    public AjaxResult update(@RequestBody DeviceDO device) {
        UpdateWrapper<DeviceDO> update = Wrappers.update(device);
        return AjaxResult.success(deviceService.update(update));
    }

    @PutMapping("/updateBatch")
    public AjaxResult update(@RequestBody List<DeviceDO> list) {
        return AjaxResult.success(deviceService.updateBatchById(list));
    }

    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteOne(@PathVariable(value = "id") Long id) {
        return AjaxResult.success(deviceService.removeById(id));
    }

    @DeleteMapping("/deleteByEntity")
    public AjaxResult deleteOne(@RequestBody DeviceDO device) {
        QueryWrapper<DeviceDO> query = Wrappers.query(device);
        return AjaxResult.success(deviceService.remove(query));
    }

    @DeleteMapping("/delete")
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
