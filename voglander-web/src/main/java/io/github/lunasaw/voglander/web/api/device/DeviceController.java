package io.github.lunasaw.voglander.web.api.device;

import java.util.List;

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
@RequestMapping("/device/api")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @GetMapping("/get/{id}")
    public AjaxResult getById(@PathVariable(value = "id") Long id) {
        DeviceDO device = deviceService.getById(id);
        return AjaxResult.success(device);
    }

    @GetMapping("/get")
    public AjaxResult getByEntity(DeviceDO device) {
        QueryWrapper<DeviceDO> query = Wrappers.query(device).last("limit 1");
        return AjaxResult.success(deviceService.getOne(query));
    }

    @GetMapping("/list")
    public AjaxResult list(DeviceDO device) {
        QueryWrapper<DeviceDO> query = Wrappers.query(device);
        List<DeviceDO> deviceList = deviceService.list(query);
        return AjaxResult.success(deviceList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    public AjaxResult listPageByEntity(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size, DeviceDO device) {
        QueryWrapper<DeviceDO> query = Wrappers.query(device);
        Page<DeviceDO> queryPage = new Page<>(page, size);
        Page<DeviceDO> pageInfo = deviceService.page(queryPage, query);
        return AjaxResult.success(pageInfo);
    }

    @GetMapping("/pageList/{page}/{size}")
    public AjaxResult listPage(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size) {
        Page<DeviceDO> queryPage = new Page<>(page, size);
        Page<DeviceDO> pageInfo = deviceService.page(queryPage);
        return AjaxResult.success(pageInfo);
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
