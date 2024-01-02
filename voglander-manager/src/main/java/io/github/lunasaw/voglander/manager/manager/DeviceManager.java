package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author luna
 * @date 2023/12/30
 */
@Component
public class DeviceManager {

    @Autowired
    private DeviceService deviceService;

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

    public void updateStatus(String deviceId, int status) {
        DeviceDO deviceDO = getByDeviceId(deviceId);
        if (deviceDO == null) {
            return;
        }
        deviceDO.setStatus(status);
        deviceService.updateById(deviceDO);
    }

    public DeviceDO getByDeviceId(String deviceId) {
        Assert.notNull(deviceId, "userId can not be null");
        QueryWrapper<DeviceDO> queryWrapper = new QueryWrapper<DeviceDO>().eq("device_id", deviceId).last("limit 1");
        return deviceService.getOne(queryWrapper);
    }

    public DeviceDTO getDtoByDeviceId(String deviceId) {
        DeviceDO byDeviceId = getByDeviceId(deviceId);
        return DeviceDTO.convertDTO(byDeviceId);
    }
}
