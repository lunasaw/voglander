package io.github.lunasaw.voglander.manager.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;

/**
 * @author luna
 * @date 2023/12/30
 */
@Component
public class DeviceManager {

    @Autowired
    private DeviceService deviceService;

    /**
     * 删除缓存，在方法之后执行
     * 
     * @param dto
     * @return
     */
    @CacheEvict(cacheNames = "device", key = "#dto.deviceId")
    public Long saveOrUpdate(DeviceDTO dto) {
        Assert.notNull(dto, "dto can not be null");
        Assert.notNull(dto.getDeviceId(), "deviceId can not be null");
        DeviceDO deviceDO = DeviceDTO.convertDO(dto);

        DeviceDO byDeviceId = getByDeviceId(dto.getDeviceId());
        if (byDeviceId != null) {
            deviceDO.setId(byDeviceId.getId());
            deviceService.updateById(deviceDO);
            return byDeviceId.getId();
        }
        deviceService.save(deviceDO);
        return deviceDO.getId();
    }

    @CacheEvict(value = "device", key = "#deviceId")
    public void updateStatus(String deviceId, int status) {
        DeviceDO deviceDO = getByDeviceId(deviceId);
        if (deviceDO == null) {
            return;
        }
        deviceDO.setStatus(status);
        deviceService.updateById(deviceDO);
    }

    /**
     * 删除缓存
     * 默认在方法执行之后进行缓存删除
     * 属性：
     * allEntries=true 时表示删除cacheNames标识的缓存下的所有缓存，默认是false
     * beforeInvocation=true 时表示在目标方法执行之前删除缓存，默认false
     */
    @CacheEvict(cacheNames = "device", key = "#deviceId")
    public Boolean deleteDevice(String deviceId) {
        Assert.notNull(deviceId, "userId can not be null");
        QueryWrapper<DeviceDO> queryWrapper = new QueryWrapper<DeviceDO>().eq("device_id", deviceId);
        return deviceService.remove(queryWrapper);
    }

    public DeviceDO getByDeviceId(String deviceId) {
        Assert.notNull(deviceId, "userId can not be null");
        QueryWrapper<DeviceDO> queryWrapper = new QueryWrapper<DeviceDO>().eq("device_id", deviceId).last("limit 1");
        return deviceService.getOne(queryWrapper);
    }

    @Cacheable(value = "device", key = "#deviceId")
    public DeviceDTO getDtoByDeviceId(String deviceId) {
        DeviceDO byDeviceId = getByDeviceId(deviceId);
        return DeviceDTO.convertDTO(byDeviceId);
    }
}
