package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备通道管理器
 * 处理设备通道相关的复杂业务逻辑
 *
 * <p>
 * 架构设计：
 * </p>
 * <ul>
 * <li>统一修改入口：{@link #deviceChannelInternal(DeviceChannelDO, String)} - 所有修改操作的核心方法</li>
 * <li>统一删除入口：{@link #deleteDeviceChannelInternal(Long, String)} - 所有删除操作的核心方法</li>
 * <li>统一缓存管理：{@link #clearDeviceChannelCache(String, String, Long)} - 统一的缓存清理逻辑</li>
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
 * @date 2023/12/31
 */
@Slf4j
@Component
public class DeviceChannelManager {

    /**
     * 设备通道缓存Key前缀
     */
    private static final String  DEVICE_CHANNEL_CACHE_PREFIX   = "device:channel:";

    /**
     * 设备通道列表缓存Key
     */
    private static final String  DEVICE_CHANNEL_LIST_CACHE_KEY = "device:channel:list";

    /**
     * 设备通道分布式锁前缀
     */
    private static final String  DEVICE_CHANNEL_LOCK_PREFIX    = "device:channel:lock:";

    @Autowired
    private DeviceChannelService deviceChannelService;

    @Autowired
    private DeviceManager deviceManager;

    @Autowired
    private RedisCache           redisCache;

    @Autowired
    private RedisLockUtil        redisLockUtil;


    /**
     * 更新设备通道状态
     *
     * @param deviceId 设备ID
     * @param channelId 通道ID
     * @param status 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String deviceId, String channelId, int status) {
        Assert.hasText(deviceId, "设备ID不能为空");
        Assert.hasText(channelId, "通道ID不能为空");

        DeviceChannelDO deviceChannelDO = getByDeviceId(deviceId, channelId);
        if (deviceChannelDO == null) {
            throw new ServiceException("设备通道不存在，设备ID: " + deviceId + ", 通道ID: " + channelId);
        }

        deviceChannelDO.setStatus(status);
        deviceChannelDO.setUpdateTime(LocalDateTime.now());

        // 调用统一入口方法
        deviceChannelInternal(deviceChannelDO, "更新设备通道状态");
    }

    /**
     * 删除设备通道
     *
     * @param deviceId 设备ID
     * @param channelId 通道ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDeviceChannel(String deviceId, String channelId) {
        Assert.hasText(deviceId, "设备ID不能为空");
        Assert.hasText(channelId, "通道ID不能为空");

        DeviceChannelDO deviceChannelDO = getByDeviceId(deviceId, channelId);
        if (deviceChannelDO == null) {
            throw new ServiceException("设备通道不存在，设备ID: " + deviceId + ", 通道ID: " + channelId);
        }

        // 调用统一删除入口方法
        return deleteDeviceChannelInternal(deviceChannelDO.getId(), "删除设备通道");
    }

    public DeviceChannelDO getByDeviceId(String deviceId, String channelId) {
        Assert.hasText(deviceId, "设备ID不能为空");
        Assert.hasText(channelId, "通道ID不能为空");
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
     *
     * @param deviceChannelDTO 设备通道DTO
     * @return 设备通道ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDeviceChannel(DeviceChannelDTO deviceChannelDTO) {
        Assert.notNull(deviceChannelDTO, "设备通道信息不能为空");
        Assert.hasText(deviceChannelDTO.getDeviceId(), "设备ID不能为空");
        Assert.hasText(deviceChannelDTO.getChannelId(), "通道ID不能为空");

        // 检查设备是否存在
        DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId(deviceChannelDTO.getDeviceId());
        if (deviceDTO == null) {
            throw new ServiceException("设备不存在: " + deviceChannelDTO.getDeviceId());
        }

        // 检查通道ID是否已存在
        DeviceChannelDO existingChannel = getByDeviceId(deviceChannelDTO.getDeviceId(), deviceChannelDTO.getChannelId());
        if (existingChannel != null) {
            throw new ServiceException("设备通道已存在: " + deviceChannelDTO.getChannelId());
        }

        DeviceChannelDO deviceChannelDO = DeviceChannelDTO.convertDO(deviceChannelDTO);
        deviceChannelDO.setCreateTime(LocalDateTime.now());

        // 调用统一入口方法
        return deviceChannelInternal(deviceChannelDO, "创建设备通道");
    }

    /**
     * 批量创建设备通道 - 使用统一入口保证缓存一致性
     *
     * @param deviceChannelDTOList 设备通道DTO列表
     * @return 成功创建的数量
     */
    @Transactional(rollbackFor = Exception.class)
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
                log.error("批量创建设备通道失败，设备ID: {}, 通道ID: {}, error: {}",
                    dto.getDeviceId(), dto.getChannelId(), e.getMessage());
            }
        }

        log.info("批量创建设备通道完成，成功创建: {} 个通道，总计: {} 个", successCount, deviceChannelDTOList.size());
        return successCount;
    }

    /**
     * 更新设备通道
     *
     * @param deviceChannelDTO 设备通道DTO
     * @return 设备通道ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long updateDeviceChannel(DeviceChannelDTO deviceChannelDTO) {
        Assert.notNull(deviceChannelDTO, "设备通道信息不能为空");
        Assert.notNull(deviceChannelDTO.getId(), "设备通道ID不能为空");

        // 检查设备通道是否存在
        DeviceChannelDO existingChannel = deviceChannelService.getById(deviceChannelDTO.getId());
        if (existingChannel == null) {
            throw new ServiceException("设备通道不存在，ID: " + deviceChannelDTO.getId());
        }

        DeviceChannelDO deviceChannelDO = DeviceChannelDTO.convertDO(deviceChannelDTO);
        deviceChannelDO.setUpdateTime(LocalDateTime.now());

        // 调用统一入口方法
        return deviceChannelInternal(deviceChannelDO, "更新设备通道");
    }

    /**
     * 批量更新设备通道 - 使用统一入口保证缓存一致性
     *
     * @param deviceChannelDTOList 设备通道DTO列表
     * @return 成功更新的数量
     */
    @Transactional(rollbackFor = Exception.class)
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
                log.error("批量更新设备通道失败，设备通道ID: {}, error: {}", dto.getId(), e.getMessage());
            }
        }

        log.info("批量更新设备通道完成，成功更新: {} 个通道，总计: {} 个", successCount, deviceChannelDTOList.size());
        return successCount;
    }

    /**
     * 设备通道数据操作统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param deviceChannelDO 设备通道数据对象
     * @param operationType 操作类型描述
     * @return 设备通道ID
     */
    private Long deviceChannelInternal(DeviceChannelDO deviceChannelDO, String operationType) {
        Assert.notNull(deviceChannelDO, "设备通道数据不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = DEVICE_CHANNEL_LOCK_PREFIX + deviceChannelDO.getDeviceId() + ":" +
            (deviceChannelDO.getChannelId() != null ? deviceChannelDO.getChannelId() : "new");

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean isUpdate = deviceChannelDO.getId() != null;
            boolean result;

            if (isUpdate) {
                result = deviceChannelService.updateById(deviceChannelDO);
            } else {
                result = deviceChannelService.save(deviceChannelDO);
            }

            if (result) {
                // 清理相关缓存
                clearDeviceChannelCache(deviceChannelDO.getDeviceId(), deviceChannelDO.getChannelId(), deviceChannelDO.getId());

                log.info("{}成功，设备ID：{}，通道ID：{}，数据库ID：{}", operationType,
                    deviceChannelDO.getDeviceId(), deviceChannelDO.getChannelId(), deviceChannelDO.getId());
                return deviceChannelDO.getId();
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，设备ID：{}，通道ID：{}，错误信息：{}", operationType,
                deviceChannelDO.getDeviceId(), deviceChannelDO.getChannelId(), e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 设备通道删除统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param deviceChannelId 设备通道数据库ID
     * @param operationType 操作类型描述
     * @return 是否删除成功
     */
    private boolean deleteDeviceChannelInternal(Long deviceChannelId, String operationType) {
        Assert.notNull(deviceChannelId, "设备通道ID不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        DeviceChannelDO existingChannel = deviceChannelService.getById(deviceChannelId);
        if (existingChannel == null) {
            throw new ServiceException("设备通道不存在，ID: " + deviceChannelId);
        }

        String lockKey = DEVICE_CHANNEL_LOCK_PREFIX + existingChannel.getDeviceId() + ":" + existingChannel.getChannelId();

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean result = deviceChannelService.removeById(deviceChannelId);

            if (result) {
                // 清理相关缓存
                clearDeviceChannelCache(existingChannel.getDeviceId(), existingChannel.getChannelId(), deviceChannelId);

                log.info("{}成功，设备ID：{}，通道ID：{}，数据库ID：{}", operationType,
                    existingChannel.getDeviceId(), existingChannel.getChannelId(), deviceChannelId);
                return true;
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，设备ID：{}，通道ID：{}，错误信息：{}", operationType,
                existingChannel.getDeviceId(), existingChannel.getChannelId(), e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 清理设备通道相关缓存
     *
     * @param deviceId 设备ID（可为null）
     * @param channelId 通道ID（可为null）
     * @param dbId 数据库ID（可为null）
     */
    private void clearDeviceChannelCache(String deviceId, String channelId, Long dbId) {
        try {
            // 清理设备通道缓存
            if (deviceId != null && channelId != null) {
                redisCache.deleteKey(DEVICE_CHANNEL_CACHE_PREFIX + deviceId + ":" + channelId);
            }
            if (dbId != null) {
                redisCache.deleteKey(DEVICE_CHANNEL_CACHE_PREFIX + dbId);
            }

            // 清理设备通道列表缓存
            redisCache.deleteKey(DEVICE_CHANNEL_LIST_CACHE_KEY);

            log.debug("清理设备通道缓存成功，设备ID：{}，通道ID：{}，数据库ID：{}", deviceId, channelId, dbId);
        } catch (Exception e) {
            log.error("清理设备通道缓存失败，设备ID：{}，通道ID：{}，数据库ID：{}，错误信息：{}", deviceId, channelId, dbId, e.getMessage());
        }
    }

    /**
     * 保存或更新设备通道 - 兼容现有调用的方法
     * 如果设备通道不存在则创建，如果存在则更新
     *
     * @param deviceChannelDTO 设备通道DTO对象
     * @return 设备通道ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdate(DeviceChannelDTO deviceChannelDTO) {
        Assert.notNull(deviceChannelDTO, "设备通道信息不能为空");
        Assert.hasText(deviceChannelDTO.getDeviceId(), "设备ID不能为空");
        Assert.hasText(deviceChannelDTO.getChannelId(), "通道ID不能为空");

        // 检查设备通道是否已存在
        DeviceChannelDO existingChannel = getByDeviceId(deviceChannelDTO.getDeviceId(), deviceChannelDTO.getChannelId());

        if (existingChannel != null) {
            // 设备通道存在，更新设备通道
            deviceChannelDTO.setId(existingChannel.getId());
            return updateDeviceChannel(deviceChannelDTO);
        } else {
            // 设备通道不存在，创建新设备通道
            return createDeviceChannel(deviceChannelDTO);
        }
    }
}
