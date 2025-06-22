package io.github.lunasaw.voglander.manager.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;


import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author luna
 * @date 2023/12/30
 */
@Slf4j
@Component
public class DeviceManager {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceAssembler deviceAssembler;
    /**
     * 创建设备
     *
     * @param deviceDTO 设备DTO对象
     * @return 设备DTO
     */
    public Long createDevice(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "deviceDTO can not be null");
        Assert.notNull(deviceDTO.getDeviceId(), "deviceId can not be null");
        Assert.notNull(deviceDTO.getIp(), "ip can not be null");
        Assert.notNull(deviceDTO.getPort(), "port can not be null");
        Assert.notNull(deviceDTO.getType(), "type can not be null");

        // 检查设备ID是否已存在
        DeviceDO existingDevice = getByDeviceId(deviceDTO.getDeviceId());
        if (existingDevice != null) {
            throw new RuntimeException("设备ID已存在: " + deviceDTO.getDeviceId());
        }

        // 设置必需的时间字段
        Date now = new Date();
        if (deviceDTO.getCreateTime() == null) {
            deviceDTO.setCreateTime(now);
        }
        if (deviceDTO.getUpdateTime() == null) {
            deviceDTO.setUpdateTime(now);
        }
        if (deviceDTO.getRegisterTime() == null) {
            deviceDTO.setRegisterTime(now);
        }
        if (deviceDTO.getKeepaliveTime() == null) {
            deviceDTO.setKeepaliveTime(now);
        }

        return saveOrUpdate(deviceDTO);
    }

    /**
     * 批量创建设备
     *
     * @param deviceDTOList 设备DTO列表
     * @return 成功创建的数量
     */
    @CacheEvict(value = "device", allEntries = true)
    public int batchCreateDevice(List<DeviceDTO> deviceDTOList) {
        if (deviceDTOList == null || deviceDTOList.isEmpty()) {
            return 0;
        }

        Date now = new Date();
        int successCount = 0;

        for (DeviceDTO deviceDTO : deviceDTOList) {
            try {
                // 检查必要字段
                Assert.notNull(deviceDTO.getDeviceId(), "deviceId can not be null");
                Assert.notNull(deviceDTO.getIp(), "ip can not be null");
                Assert.notNull(deviceDTO.getPort(), "port can not be null");
                Assert.notNull(deviceDTO.getType(), "type can not be null");

                // 检查设备ID是否已存在
                DeviceDO existingDevice = getByDeviceId(deviceDTO.getDeviceId());
                if (existingDevice != null) {
                    log.warn("设备ID已存在，跳过创建: {}", deviceDTO.getDeviceId());
                    continue;
                }

                // 设置创建时间和更新时间
                deviceDTO.setCreateTime(now);
                deviceDTO.setUpdateTime(now);

                // DTO -> DO
                DeviceDO deviceDO = deviceAssembler.toDeviceDO(deviceDTO);

                // 保存到数据库
                if (deviceService.save(deviceDO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量创建设备失败，deviceId: {}, error: {}", deviceDTO.getDeviceId(), e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * 更新设备
     *
     * @param deviceDTO 设备DTO对象
     * @return 更新后的设备DTO
     */
    public Long updateDevice(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "deviceDTO can not be null");
        Assert.notNull(deviceDTO.getId(), "id can not be null");

        // 检查设备是否存在
        DeviceDTO existingDevice = getDeviceDTOById(deviceDTO.getId());
        if (existingDevice == null) {
            throw new RuntimeException("设备不存在，ID: " + deviceDTO.getId());
        }

        return saveOrUpdate(deviceDTO);
    }

    /**
     * 批量更新设备
     *
     * @param deviceDTOList 设备DTO列表
     * @return 成功更新的数量
     */
    @CacheEvict(value = "device", allEntries = true)
    public int batchUpdateDevice(List<DeviceDTO> deviceDTOList) {
        if (deviceDTOList == null || deviceDTOList.isEmpty()) {
            return 0;
        }

        Date now = new Date();
        int successCount = 0;

        for (DeviceDTO deviceDTO : deviceDTOList) {
            try {
                // 检查必要字段
                Assert.notNull(deviceDTO.getId(), "id can not be null");

                // 检查设备是否存在
                DeviceDTO existingDevice = getDeviceDTOById(deviceDTO.getId());
                if (existingDevice == null) {
                    log.warn("设备不存在，跳过更新: ID {}", deviceDTO.getId());
                    continue;
                }

                // 设置更新时间
                deviceDTO.setUpdateTime(now);

                // 保留原有的时间字段
                if (deviceDTO.getCreateTime() == null) {
                    deviceDTO.setCreateTime(existingDevice.getCreateTime());
                }
                if (deviceDTO.getRegisterTime() == null) {
                    deviceDTO.setRegisterTime(existingDevice.getRegisterTime());
                }
                if (deviceDTO.getKeepaliveTime() == null) {
                    deviceDTO.setKeepaliveTime(existingDevice.getKeepaliveTime());
                }

                // DTO -> DO
                DeviceDO deviceDO = deviceAssembler.toDeviceDO(deviceDTO);

                // 更新到数据库
                if (deviceService.updateById(deviceDO)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量更新设备失败，ID: {}, error: {}", deviceDTO.getId(), e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * 删除缓存，在方法之后执行
     *
     * @param dto
     * @return
     */
    @CacheEvict(value = "device", key = "#dto.deviceId")
    public Long saveOrUpdate(DeviceDTO dto) {
        Assert.notNull(dto, "dto can not be null");
        Assert.notNull(dto.getDeviceId(), "deviceId can not be null");
        DeviceDO deviceDO = deviceAssembler.toDeviceDO(dto);

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
    @CacheEvict(value = "device", key = "#deviceId")
    public Boolean deleteDevice(String deviceId) {
        Assert.notNull(deviceId, "userId can not be null");
        QueryWrapper<DeviceDO> queryWrapper = new QueryWrapper<DeviceDO>().eq("device_id", deviceId);
        return deviceService.remove(queryWrapper);
    }

    public DeviceDO getByDeviceId(String deviceId) {
        Assert.notNull(deviceId, "userId can not be null");
        QueryWrapper<DeviceDO> queryWrapper = new QueryWrapper<DeviceDO>().eq("device_id", deviceId);
        return deviceService.getOne(queryWrapper);
    }

    @Cacheable(value = "device", key = "#deviceId", unless = "#result == null")
    public DeviceDTO getDtoByDeviceId(String deviceId) {
        DeviceDO byDeviceId = getByDeviceId(deviceId);
        return deviceAssembler.toDeviceDTO(byDeviceId);
    }

    /**
     * 根据ID获取设备DTO
     *
     * @param id 设备主键ID
     * @return DeviceDTO
     */
    public DeviceDTO getDeviceDTOById(Long id) {
        DeviceDO deviceDO = deviceService.getById(id);
        return deviceAssembler.toDeviceDTO(deviceDO);
    }

    /**
     * 根据实体条件获取单个设备DTO
     *
     * @param device 查询条件
     * @return DeviceDTO
     */
    public DeviceDTO getDeviceDTOByEntity(DeviceDO device) {
        QueryWrapper<DeviceDO> query = new QueryWrapper<>();
        if (device.getDeviceId() != null) {
            query.eq("device_id", device.getDeviceId());
        }
        if (device.getName() != null) {
            query.eq("name", device.getName());
        }
        if (device.getStatus() != null) {
            query.eq("status", device.getStatus());
        }
        if (device.getType() != null) {
            query.eq("type", device.getType());
        }

        DeviceDO deviceDO = deviceService.getOne(query);
        return deviceAssembler.toDeviceDTO(deviceDO);
    }

    /**
     * 根据条件查询设备DTO列表
     *
     * @param device 查询条件
     * @return DeviceDTO列表
     */
    public List<DeviceDTO> listDeviceDTO(DeviceDO device) {
        QueryWrapper<DeviceDO> query = new QueryWrapper<>();
        if (device != null) {
            if (device.getDeviceId() != null) {
                query.eq("device_id", device.getDeviceId());
            }
            if (device.getName() != null) {
                query.like("name", device.getName());
            }
            if (device.getStatus() != null) {
                query.eq("status", device.getStatus());
            }
            if (device.getType() != null) {
                query.eq("type", device.getType());
            }
        }

        List<DeviceDO> deviceDOList = deviceService.list(query);
        return deviceAssembler.toDeviceDTOList(deviceDOList);
    }

    /**
     * 简单分页查询设备DTO列表
     *
     * @param page 当前页
     * @param size 页大小
     * @return 分页结果
     */
    public Page<DeviceDTO> pageQuerySimple(int page, int size) {
        Page<DeviceDO> queryPage = new Page<>(page, size);
        Page<DeviceDO> pageInfo = deviceService.page(queryPage);

        // 使用 Assembler 进行数据转换
        Page<DeviceDTO> resultPage = new Page<>(page, size);
        resultPage.setRecords(deviceAssembler.toDeviceDTOList(pageInfo.getRecords()));
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return resultPage;
    }

    /**
     * 分页查询设备列表，返回DTO模型并解析扩展字段
     *
     * @param page 当前页
     * @param size 页大小
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    public Page<DeviceDTO> pageQuery(int page, int size, QueryWrapper<DeviceDO> queryWrapper) {
        Page<DeviceDO> queryPage = new Page<>(page, size);
        Page<DeviceDO> pageInfo = deviceService.page(queryPage, queryWrapper);

        // 使用 Assembler 进行数据转换
        Page<DeviceDTO> resultPage = new Page<>(page, size);
        resultPage.setRecords(deviceAssembler.toDeviceDTOList(pageInfo.getRecords()));
        resultPage.setTotal(pageInfo.getTotal());
        resultPage.setCurrent(pageInfo.getCurrent());
        resultPage.setSize(pageInfo.getSize());
        resultPage.setPages(pageInfo.getPages());

        return resultPage;
    }
}
