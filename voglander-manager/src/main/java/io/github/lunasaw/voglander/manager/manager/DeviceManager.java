package io.github.lunasaw.voglander.manager.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luna.common.check.Assert;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;

import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * 设备管理器
 * 处理设备相关的复杂业务逻辑
 *
 * <p>
 * 架构设计：
 * </p>
 * <ul>
 * <li>统一修改入口：{@link #deviceInternal(DeviceDO, String)} - 所有修改操作的核心方法</li>
 * <li>统一删除入口：{@link #deleteDeviceInternal(Long, String)} - 所有删除操作的核心方法</li>
 * <li>统一缓存管理：{@link #clearDeviceCache(String, Long)} - 统一的缓存清理逻辑</li>
 * </ul>
 *
 * <p>
 * 优势：
 * </p>
 * <ul>
 * <li>缓存一致性：所有修改和删除操作都通过统一入口，确保缓存清理的一致性</li>
 * <li>日志规范：统一的日志记录格式和内容</li>
 * <li>异常处理：统一的数据校验和异常处理逻辑</li>
 * <li>维护便捷：业务逻辑变更只需修改核心方法</li>
 * </ul>
 *
 * @author luna
 * @date 2023/12/30
 */
@Slf4j
@Component
public class DeviceManager {

    /**
     * 设备缓存Key前缀
     */
    private static final String DEVICE_CACHE_PREFIX   = "device:";

    /**
     * 设备列表缓存Key
     */
    private static final String DEVICE_LIST_CACHE_KEY = "device:list";

    /**
     * 设备分布式锁前缀
     */
    private static final String DEVICE_LOCK_PREFIX    = "device:lock:";

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceAssembler deviceAssembler;

    @Autowired
    private RedisCache          redisCache;

    @Autowired
    private RedisLockUtil       redisLockUtil;
    
    /**
     * 创建设备
     *
     * @param deviceDTO 设备DTO对象
     * @return 设备ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "设备信息不能为空");
        Assert.hasText(deviceDTO.getDeviceId(), "设备ID不能为空");
        Assert.hasText(deviceDTO.getIp(), "设备IP不能为空");
        Assert.notNull(deviceDTO.getPort(), "设备端口不能为空");
        Assert.notNull(deviceDTO.getType(), "设备类型不能为空");

        // 检查设备ID是否已存在
        DeviceDO existingDevice = getByDeviceId(deviceDTO.getDeviceId());
        if (existingDevice != null) {
            throw new ServiceException("设备ID已存在: " + deviceDTO.getDeviceId());
        }

        DeviceDO deviceDO = deviceAssembler.toDeviceDO(deviceDTO);
        deviceDO.setCreateTime(LocalDateTime.now());

        // 调用统一入口方法
        return deviceInternal(deviceDO, "创建设备");
    }

    /**
     * 批量创建设备 - 使用统一入口保证缓存一致性
     *
     * @param deviceDTOList 设备DTO列表
     * @return 成功创建的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateDevice(List<DeviceDTO> deviceDTOList) {
        if (deviceDTOList == null || deviceDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;

        for (DeviceDTO deviceDTO : deviceDTOList) {
            try {
                // 检查必要字段
                Assert.hasText(deviceDTO.getDeviceId(), "设备ID不能为空");
                Assert.hasText(deviceDTO.getIp(), "设备IP不能为空");
                Assert.notNull(deviceDTO.getPort(), "设备端口不能为空");
                Assert.notNull(deviceDTO.getType(), "设备类型不能为空");

                // 检查设备ID是否已存在
                DeviceDO existingDevice = getByDeviceId(deviceDTO.getDeviceId());
                if (existingDevice != null) {
                    log.warn("设备ID已存在，跳过创建: {}", deviceDTO.getDeviceId());
                    continue;
                }

                DeviceDO deviceDO = deviceAssembler.toDeviceDO(deviceDTO);
                deviceDO.setCreateTime(LocalDateTime.now());

                // 调用统一入口方法
                deviceInternal(deviceDO, "批量创建设备");
                successCount++;
            } catch (Exception e) {
                log.error("批量创建设备失败，deviceId: {}, error: {}", deviceDTO.getDeviceId(), e.getMessage());
            }
        }

        log.info("批量创建设备完成，成功创建: {} 个设备，总计: {} 个", successCount, deviceDTOList.size());
        return successCount;
    }

    /**
     * 更新设备
     *
     * @param deviceDTO 设备DTO对象
     * @return 设备ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long updateDevice(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "设备信息不能为空");
        Assert.notNull(deviceDTO.getId(), "设备ID不能为空");

        // 检查设备是否存在
        DeviceDO existingDevice = deviceService.getById(deviceDTO.getId());
        if (existingDevice == null) {
            throw new ServiceException("设备不存在，ID: " + deviceDTO.getId());
        }

        DeviceDO deviceDO = deviceAssembler.toDeviceDO(deviceDTO);
        deviceDO.setUpdateTime(LocalDateTime.now());

        // 调用统一入口方法
        return deviceInternal(deviceDO, "更新设备");
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
     * 更新设备状态
     *
     * @param deviceId 设备ID
     * @param status 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String deviceId, int status) {
        Assert.hasText(deviceId, "设备ID不能为空");

        DeviceDO deviceDO = getByDeviceId(deviceId);
        if (deviceDO == null) {
            throw new ServiceException("设备不存在: " + deviceId);
        }

        deviceDO.setStatus(status);
        deviceDO.setUpdateTime(LocalDateTime.now());

        // 调用统一入口方法
        deviceInternal(deviceDO, "更新设备状态");
    }

    /**
     * 删除设备
     *
     * @param deviceId 设备ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDevice(String deviceId) {
        Assert.hasText(deviceId, "设备ID不能为空");

        DeviceDO deviceDO = getByDeviceId(deviceId);
        if (deviceDO == null) {
            throw new ServiceException("设备不存在: " + deviceId);
        }

        // 调用统一删除入口方法
        return deleteDeviceInternal(deviceDO.getId(), "删除设备");
    }

    public DeviceDO getByDeviceId(String deviceId) {
        Assert.hasText(deviceId, "设备ID不能为空");
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

    /**
     * 设备数据操作统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param deviceDO 设备数据对象
     * @param operationType 操作类型描述
     * @return 设备ID
     */
    private Long deviceInternal(DeviceDO deviceDO, String operationType) {
        Assert.notNull(deviceDO, "设备数据不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = DEVICE_LOCK_PREFIX + (deviceDO.getDeviceId() != null ? deviceDO.getDeviceId() : "new");

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean isUpdate = deviceDO.getId() != null;
            boolean result;

            if (isUpdate) {
                result = deviceService.updateById(deviceDO);
            } else {
                result = deviceService.save(deviceDO);
            }

            if (result) {
                // 清理相关缓存
                clearDeviceCache(deviceDO.getDeviceId(), deviceDO.getId());

                log.info("{}成功，设备ID：{}，数据库ID：{}", operationType, deviceDO.getDeviceId(), deviceDO.getId());
                return deviceDO.getId();
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，设备ID：{}，错误信息：{}", operationType, deviceDO.getDeviceId(), e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 设备删除统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param deviceId 设备数据库ID
     * @param operationType 操作类型描述
     * @return 是否删除成功
     */
    private boolean deleteDeviceInternal(Long deviceId, String operationType) {
        Assert.notNull(deviceId, "设备ID不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        DeviceDO existingDevice = deviceService.getById(deviceId);
        if (existingDevice == null) {
            throw new ServiceException("设备不存在，ID: " + deviceId);
        }

        String lockKey = DEVICE_LOCK_PREFIX + existingDevice.getDeviceId();

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean result = deviceService.removeById(deviceId);

            if (result) {
                // 清理相关缓存
                clearDeviceCache(existingDevice.getDeviceId(), deviceId);

                log.info("{}成功，设备ID：{}，数据库ID：{}", operationType, existingDevice.getDeviceId(), deviceId);
                return true;
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，设备ID：{}，错误信息：{}", operationType, existingDevice.getDeviceId(), e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 清理设备相关缓存
     *
     * @param deviceId 设备ID（可为null）
     * @param dbId 数据库ID（可为null）
     */
    private void clearDeviceCache(String deviceId, Long dbId) {
        try {
            // 清理设备缓存
            if (deviceId != null) {
                redisCache.deleteKey(DEVICE_CACHE_PREFIX + deviceId);
            }
            if (dbId != null) {
                redisCache.deleteKey(DEVICE_CACHE_PREFIX + dbId);
            }

            // 清理设备列表缓存
            redisCache.deleteKey(DEVICE_LIST_CACHE_KEY);

            log.debug("清理设备缓存成功，设备ID：{}，数据库ID：{}", deviceId, dbId);
        } catch (Exception e) {
            log.error("清理设备缓存失败，设备ID：{}，数据库ID：{}，错误信息：{}", deviceId, dbId, e.getMessage());
        }
    }
}
