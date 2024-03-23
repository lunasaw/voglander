package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author luna
 * @date 2023/12/31
 */
@Component
public class DeviceChannelManager {

    @Autowired
    private DeviceChannelService deviceChannelService;

    @Autowired
    private DeviceManager deviceManager;

    public Long saveOrUpdate(DeviceChannelDTO dto) {
        Assert.notNull(dto, "dto can not be null");
        Assert.notNull(dto.getDeviceId(), "deviceId can not be null");

        DeviceDTO dtoByDeviceId = deviceManager.getDtoByDeviceId(dto.getDeviceId());
        if (dtoByDeviceId == null) {
            return null;
        }

        DeviceChannelDO deviceChannelDO = DeviceChannelDTO.convertDO(dto);
        DeviceChannelDO byDeviceId = getByDeviceId(dto.getDeviceId(), dto.getChannelId());
        if (byDeviceId != null) {
            deviceChannelDO.setId(byDeviceId.getId());
            deviceChannelService.updateById(deviceChannelDO);
            return byDeviceId.getId();
        }
        deviceChannelService.save(deviceChannelDO);
        return deviceChannelDO.getId();
    }

    public void updateStatus(String deviceId, String channelId, int status) {
        DeviceChannelDO DeviceChannelDO = getByDeviceId(deviceId, channelId);
        if (DeviceChannelDO == null) {
            return;
        }
        DeviceChannelDO.setStatus(status);
        deviceChannelService.updateById(DeviceChannelDO);
    }

    public DeviceChannelDO getByDeviceId(String deviceId, String channelId) {
        Assert.notNull(deviceId, "userId can not be null");
        QueryWrapper<DeviceChannelDO> queryWrapper = new QueryWrapper<DeviceChannelDO>().eq("device_id", deviceId)
                .eq("channel_id", channelId).last("limit 1");
        return deviceChannelService.getOne(queryWrapper);
    }

    public DeviceChannelDTO getDtoByDeviceId(String deviceId, String channelId) {
        DeviceChannelDO byDeviceId = getByDeviceId(deviceId, channelId);
        return DeviceChannelDTO.convertDTO(byDeviceId);
    }
}
