package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * 根据ID获取设备通道DTO
     */
    public DeviceChannelDTO getDeviceChannelDTOById(Long id) {
        DeviceChannelDO deviceChannelDO = deviceChannelService.getById(id);
        return DeviceChannelDTO.convertDTO(deviceChannelDO);
    }

    /**
     * 根据条件获取设备通道DTO
     */
    public DeviceChannelDTO getDeviceChannelDTOByEntity(DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = new QueryWrapper<>(deviceChannel).last("limit 1");
        DeviceChannelDO deviceChannelDO = deviceChannelService.getOne(query);
        return DeviceChannelDTO.convertDTO(deviceChannelDO);
    }

    /**
     * 获取设备通道DTO列表
     */
    public List<DeviceChannelDTO> listDeviceChannelDTO(DeviceChannelDO deviceChannel) {
        QueryWrapper<DeviceChannelDO> query = new QueryWrapper<>(deviceChannel);
        List<DeviceChannelDO> deviceChannelList = deviceChannelService.list(query);
        return deviceChannelList.stream()
                .map(DeviceChannelDTO::convertDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询设备通道DTO
     */
    public Page<DeviceChannelDTO> pageQuery(int page, int size, QueryWrapper<DeviceChannelDO> query) {
        Page<DeviceChannelDO> queryPage = new Page<>(page, size);
        Page<DeviceChannelDO> pageInfo = deviceChannelService.page(queryPage, query);

        // 转换为DTO分页对象
        Page<DeviceChannelDTO> resultPage = new Page<>(page, size);
        List<DeviceChannelDTO> dtoList = pageInfo.getRecords().stream()
                .map(DeviceChannelDTO::convertDTO)
                .collect(Collectors.toList());

        resultPage.setRecords(dtoList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return resultPage;
    }

    /**
     * 简单分页查询设备通道DTO
     */
    public Page<DeviceChannelDTO> pageQuerySimple(int page, int size) {
        Page<DeviceChannelDO> queryPage = new Page<>(page, size);
        Page<DeviceChannelDO> pageInfo = deviceChannelService.page(queryPage);

        // 转换为DTO分页对象
        Page<DeviceChannelDTO> resultPage = new Page<>(page, size);
        List<DeviceChannelDTO> dtoList = pageInfo.getRecords().stream()
                .map(DeviceChannelDTO::convertDTO)
                .collect(Collectors.toList());

        resultPage.setRecords(dtoList);
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return resultPage;
    }

    /**
     * 创建设备通道
     */
    public Long createDeviceChannel(DeviceChannelDTO deviceChannelDTO) {
        Assert.notNull(deviceChannelDTO, "deviceChannelDTO can not be null");
        Assert.notNull(deviceChannelDTO.getDeviceId(), "deviceId can not be null");
        Assert.notNull(deviceChannelDTO.getChannelId(), "channelId can not be null");

        // 检查设备是否存在
        DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId(deviceChannelDTO.getDeviceId());
        if (deviceDTO == null) {
            throw new RuntimeException("设备不存在: " + deviceChannelDTO.getDeviceId());
        }

        // 检查通道ID是否已存在
        DeviceChannelDO existingChannel = getByDeviceId(deviceChannelDTO.getDeviceId(), deviceChannelDTO.getChannelId());
        if (existingChannel != null) {
            throw new RuntimeException("设备通道已存在: " + deviceChannelDTO.getChannelId());
        }

        // 设置必需的时间字段
        Date now = new Date();
        if (deviceChannelDTO.getCreateTime() == null) {
            deviceChannelDTO.setCreateTime(now);
        }
        if (deviceChannelDTO.getUpdateTime() == null) {
            deviceChannelDTO.setUpdateTime(now);
        }

        return saveOrUpdate(deviceChannelDTO);
    }

    /**
     * 批量创建设备通道
     */
    public int batchCreateDeviceChannel(List<DeviceChannelDTO> deviceChannelDTOList) {
        if (deviceChannelDTOList == null || deviceChannelDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (DeviceChannelDTO dto : deviceChannelDTOList) {
            try {
                createDeviceChannel(dto);
                successCount++;
            } catch (Exception e) {
                // 记录日志但不中断处理
                // log.warn("批量创建设备通道失败: {}", e.getMessage());
            }
        }
        return successCount;
    }

    /**
     * 更新设备通道
     */
    public Long updateDeviceChannel(DeviceChannelDTO deviceChannelDTO) {
        Assert.notNull(deviceChannelDTO, "deviceChannelDTO can not be null");
        Assert.notNull(deviceChannelDTO.getId(), "id can not be null");

        // 设置更新时间
        deviceChannelDTO.setUpdateTime(new Date());

        DeviceChannelDO deviceChannelDO = DeviceChannelDTO.convertDO(deviceChannelDTO);
        deviceChannelService.updateById(deviceChannelDO);
        return deviceChannelDTO.getId();
    }

    /**
     * 批量更新设备通道
     */
    public int batchUpdateDeviceChannel(List<DeviceChannelDTO> deviceChannelDTOList) {
        if (deviceChannelDTOList == null || deviceChannelDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (DeviceChannelDTO dto : deviceChannelDTOList) {
            try {
                updateDeviceChannel(dto);
                successCount++;
            } catch (Exception e) {
                // 记录日志但不中断处理
                // log.warn("批量更新设备通道失败: {}", e.getMessage());
            }
        }
        return successCount;
    }
}
