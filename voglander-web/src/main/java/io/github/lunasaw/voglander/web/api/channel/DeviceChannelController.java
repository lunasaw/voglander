package io.github.lunasaw.voglander.web.api.channel;

import java.util.List;

import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.domain.AjaxResult;

/**
 * @Author: chenzhangyue
 * @CreateTime: 2024-01-30 14:20:37
 */
@RestController
@RequestMapping("/deviceChannel/api")
public class DeviceChannelController {

    @Autowired
    private DeviceChannelService deviceChannelService;

    @GetMapping("/get/{id}")
    public AjaxResult getById(@PathVariable(value = "id") Long id) {
        DeviceChannelDO deviceChannel = deviceChannelService.getById(id);
        return AjaxResult.success(deviceChannel);
    }

    @GetMapping("/get")
    public AjaxResult getByEntity(DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = Wrappers.query(deviceChannel).last("limit 1");
        return AjaxResult.success(deviceChannelService.getOne(query));
    }

    @GetMapping("/list")
    public AjaxResult list(DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = Wrappers.query(deviceChannel);
        List<DeviceChannelDO> deviceChannelList = deviceChannelService.list(query);
        return AjaxResult.success(deviceChannelList);
    }

    @GetMapping("/pageListByEntity/{page}/{size}")
    public AjaxResult listPageByEntity(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size,
        DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = Wrappers.query(deviceChannel);
        Page<DeviceChannelDO> queryPage = new Page<>(page, size);
        Page<DeviceChannelDO> pageInfo = deviceChannelService.page(queryPage, query);
        return AjaxResult.success(pageInfo);
    }

    @GetMapping("/pageList/{page}/{size}")
    public AjaxResult listPage(@PathVariable(value = "page") int page, @PathVariable(value = "size") int size) {
        Page<DeviceChannelDO> queryPage = new Page<>(page, size);
        Page<DeviceChannelDO> pageInfo = deviceChannelService.page(queryPage);
        return AjaxResult.success(pageInfo);
    }

    @PostMapping("/insert")
    public AjaxResult insert(@RequestBody DeviceChannelDO deviceChannel) {
        deviceChannelService.save(deviceChannel);
        return AjaxResult.success(deviceChannel);
    }

    @PostMapping("/insertBatch")
    public AjaxResult insert(@RequestBody List<DeviceChannelDO> list) {
        boolean saved = deviceChannelService.saveBatch(list);
        return AjaxResult.success(saved);
    }

    @PutMapping("/update")
    public AjaxResult update(@RequestBody DeviceChannelDO deviceChannel) {
        UpdateWrapper<DeviceChannelDO> update = Wrappers.update(deviceChannel);
        return AjaxResult.success(deviceChannelService.update(update));
    }

    @PutMapping("/updateBatch")
    public AjaxResult update(@RequestBody List<DeviceChannelDO> list) {
        return AjaxResult.success(deviceChannelService.updateBatchById(list));
    }

    @DeleteMapping("/delete/{id}")
    public AjaxResult deleteOne(@PathVariable(value = "id") Long id) {
        return AjaxResult.success(deviceChannelService.removeById(id));
    }

    @DeleteMapping("/deleteByEntity")
    public AjaxResult deleteOne(@RequestBody DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = Wrappers.query(deviceChannel);
        return AjaxResult.success(deviceChannelService.remove(query));
    }

    @DeleteMapping("/delete")
    public AjaxResult deleteBatch(@RequestBody List<Long> ids) {
        return AjaxResult.success(deviceChannelService.removeBatchByIds(ids));
    }

    @GetMapping("/count")
    public AjaxResult getAccount() {
        return AjaxResult.success(deviceChannelService.count());
    }

    @GetMapping("/countByEntity")
    public AjaxResult getAccountByEntity(DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = Wrappers.query(deviceChannel);
        return AjaxResult.success(deviceChannelService.count(query));
    }
}
