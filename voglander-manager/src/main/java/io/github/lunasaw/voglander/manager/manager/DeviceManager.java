package io.github.lunasaw.voglander.manager.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luna.common.check.Assert;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.assembler.DeviceAssembler;
import io.github.lunasaw.voglander.manager.cache.DelayedCacheEviction;
import io.github.lunasaw.voglander.manager.cache.DeviceCacheKey;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

/**
 * 设备管理器
 * 遵循标准Manager层模板方法设计模式
 *
 * <p>
 * 核心模板方法：
 * </p>
 * <ul>
 * <li>{@link #add(DeviceDTO)} - 标准新增模板</li>
 * <li>{@link #update(DeviceDTO, DeviceDTO)} - 条件更新模板</li>
 * <li>{@link #updateById(Long, DeviceDTO)} - ID更新扩展方法</li>
 * <li>{@link #get(DeviceDTO)} - 单条查询模板</li>
 * <li>{@link #deleteOne(DeviceDTO)} - 单条删除模板</li>
 * <li>{@link #deleteBatch(DeviceDTO)} - 批量删除模板</li>
 * <li>{@link #getPage(DeviceDTO, int, int)} - 分页查询模板</li>
 * </ul>
 *
 * <p>
 * 兼容方法（保持Controller层接口兼容）：
 * </p>
 * <ul>
 * <li>{@link #createDevice(DeviceDTO)} - 兼容原创建接口</li>
 * <li>{@link #updateDevice(DeviceDTO)} - 兼容原更新接口</li>
 * <li>{@link #saveOrUpdate(DeviceDTO)} - 兼容原保存更新接口</li>
 * <li>其他查询方法保持不变</li>
 * </ul>
 *
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

    @Autowired
    private CacheManager        cacheManager;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private io.github.lunasaw.voglander.manager.routing.DeviceNodeRouteService deviceNodeRouteService;

    private DelayedCacheEviction delayedEviction;

    /**
     * 心跳合并缓存：记录每个设备上次持久化的时间戳
     * Key: deviceId, Value: lastPersistTimestamp (milliseconds)
     */
    private final ConcurrentHashMap<String, Long> lastPersistTs = new ConcurrentHashMap<>();

    /**
     * 心跳合并窗口（毫秒）
     * 在此窗口内的心跳只更新缓存，不写数据库
     * 默认 30000ms (30秒)
     */
    private long coalesceWindowMs = 30000L;

    // ================================
    // 核心模板方法
    // ================================

    /**
     * 模板方法：新增数据
     * 标准流程：校验参数 -> 转换DO -> 插入数据库 -> 返回ID
     * 注意：默认值依赖数据库字段默认值，不在代码中设置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long add(DeviceDTO deviceDTO) {
        // 校验必要参数
        Assert.notNull(deviceDTO, "设备信息不能为空");
        Assert.hasText(deviceDTO.getDeviceId(), "设备ID不能为空");
        Assert.hasText(deviceDTO.getIp(), "设备IP不能为空");
        Assert.notNull(deviceDTO.getPort(), "设备端口不能为空");
        Assert.notNull(deviceDTO.getType(), "设备类型不能为空");

        // 检查设备ID是否已存在
        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setDeviceId(deviceDTO.getDeviceId());
        DeviceDTO existingDevice = get(queryDTO);
        if (existingDevice != null) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION, "设备ID已存在: " + deviceDTO.getDeviceId());
        }

        try {
            // 转为DO
            DeviceDO deviceDO = deviceAssembler.toDeviceDO(deviceDTO);
            deviceDO.setCreateTime(LocalDateTime.now());
            deviceDO.setUpdateTime(LocalDateTime.now());

            // 插入DB（依赖数据库默认值）
            boolean success = deviceService.save(deviceDO);
            if (!success) {
                throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, "数据库插入失败");
            }

            // 清理相关缓存
            clearCache(deviceDO.getId(), null, deviceDTO.getDeviceId());

            return deviceDO.getId();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("新增设备失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：条件更新数据（通用版本）
     * 标准流程：校验参数 -> 根据查询条件查找记录 -> 应用更新内容 -> 更新数据库
     * 查询条件和更新内容完全分离，提供最大的灵活性
     */
    @Transactional(rollbackFor = Exception.class)
    public Long update(DeviceDTO queryDTO, DeviceDTO updateDTO) {
        Assert.notNull(queryDTO, "查询条件不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        try {
            LambdaQueryWrapper<DeviceDO> queryWrapper = new LambdaQueryWrapper<>();

            // 使用condition参数避免空值判断，只有非空时才添加条件
            queryWrapper.eq(queryDTO.getId() != null, DeviceDO::getId, queryDTO.getId())
                .eq(queryDTO.getDeviceId() != null, DeviceDO::getDeviceId, queryDTO.getDeviceId())
                .eq(queryDTO.getStatus() != null, DeviceDO::getStatus, queryDTO.getStatus())
                .eq(queryDTO.getName() != null, DeviceDO::getName, queryDTO.getName())
                .eq(queryDTO.getIp() != null, DeviceDO::getIp, queryDTO.getIp())
                .eq(queryDTO.getPort() != null, DeviceDO::getPort, queryDTO.getPort())
                .eq(queryDTO.getServerIp() != null, DeviceDO::getServerIp, queryDTO.getServerIp())
                .eq(queryDTO.getType() != null, DeviceDO::getType, queryDTO.getType())
                .last("limit 1");

            DeviceDO existingRecord = deviceService.getOne(queryWrapper);
            if (existingRecord == null) {
                throw new ServiceException(ServiceExceptionEnum.DEVICE_NOT_FOUND, "未找到要更新的记录");
            }

            String oldDeviceId = existingRecord.getDeviceId();

            // 应用更新内容
            DeviceDO updateDO = deviceAssembler.toDeviceDO(updateDTO);
            updateDO.setId(existingRecord.getId());
            updateDO.setUpdateTime(LocalDateTime.now());

            // 保留不更新的字段
            if (updateDTO.getCreateTime() == null) {
                updateDO.setCreateTime(existingRecord.getCreateTime());
            }

            boolean success = deviceService.updateById(updateDO);
            if (!success) {
                throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, "数据库更新失败");
            }

            clearCache(updateDO.getId(), oldDeviceId, updateDO.getDeviceId());
            return updateDO.getId();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("条件更新设备失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 扩展方法：通过ID更新指定字段
     * 最常用的更新方式，直接通过主键ID更新指定字段
     */
    @Transactional(rollbackFor = Exception.class)
    public Long updateById(Long id, DeviceDTO updateDTO) {
        Assert.notNull(id, "设备ID不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        try {
            DeviceDO existingRecord = deviceService.getById(id);
            if (existingRecord == null) {
                throw new ServiceException(ServiceExceptionEnum.DEVICE_NOT_FOUND, "设备不存在，ID: " + id);
            }

            String oldDeviceId = existingRecord.getDeviceId();

            DeviceDO updateDO = deviceAssembler.toDeviceDO(updateDTO);
            updateDO.setId(id);
            updateDO.setUpdateTime(LocalDateTime.now());

            // 保留不更新的字段
            if (updateDTO.getCreateTime() == null) {
                updateDO.setCreateTime(existingRecord.getCreateTime());
            }

            boolean success = deviceService.updateById(updateDO);
            if (!success) {
                throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, "数据库更新失败");
            }

            clearCache(id, oldDeviceId, updateDO.getDeviceId());
            return id;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("通过ID更新设备失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：单条查询
     * 标准流程：校验参数 -> 转换DO条件 -> 查询数据库 -> 转换并返回DTO
     * 查询实现：使用LambdaQueryWrapper进行类型安全的条件构建
     */
    public DeviceDTO get(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "查询条件不能为空");

        try {
            LambdaQueryWrapper<DeviceDO> queryWrapper = new LambdaQueryWrapper<>();

            // 使用condition参数避免空值判断，只有非空时才添加条件
            queryWrapper.eq(deviceDTO.getId() != null, DeviceDO::getId, deviceDTO.getId())
                .eq(deviceDTO.getDeviceId() != null, DeviceDO::getDeviceId, deviceDTO.getDeviceId())
                .eq(deviceDTO.getStatus() != null, DeviceDO::getStatus, deviceDTO.getStatus())
                .eq(deviceDTO.getName() != null, DeviceDO::getName, deviceDTO.getName())
                .eq(deviceDTO.getIp() != null, DeviceDO::getIp, deviceDTO.getIp())
                .eq(deviceDTO.getPort() != null, DeviceDO::getPort, deviceDTO.getPort())
                .eq(deviceDTO.getServerIp() != null, DeviceDO::getServerIp, deviceDTO.getServerIp())
                .eq(deviceDTO.getType() != null, DeviceDO::getType, deviceDTO.getType())
                .last("limit 1");

            DeviceDO existingRecord = deviceService.getOne(queryWrapper);
            if (existingRecord == null) {
                return null;
            }

            return deviceAssembler.toDeviceDTO(existingRecord);
        } catch (Exception e) {
            log.error("查询设备失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：删除单条记录
     * 标准流程：校验参数 -> 通过条件查找 -> 删除数据库记录 -> 清理缓存
     * 删除实现：使用LambdaQueryWrapper构建查询条件
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteOne(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "删除条件不能为空");

        try {
            LambdaQueryWrapper<DeviceDO> queryWrapper = new LambdaQueryWrapper<>();

            // 使用condition参数避免空值判断，只有非空时才添加条件
            queryWrapper.eq(deviceDTO.getId() != null, DeviceDO::getId, deviceDTO.getId())
                .eq(deviceDTO.getDeviceId() != null, DeviceDO::getDeviceId, deviceDTO.getDeviceId())
                .eq(deviceDTO.getStatus() != null, DeviceDO::getStatus, deviceDTO.getStatus())
                .eq(deviceDTO.getName() != null, DeviceDO::getName, deviceDTO.getName())
                .eq(deviceDTO.getIp() != null, DeviceDO::getIp, deviceDTO.getIp())
                .eq(deviceDTO.getPort() != null, DeviceDO::getPort, deviceDTO.getPort())
                .eq(deviceDTO.getServerIp() != null, DeviceDO::getServerIp, deviceDTO.getServerIp())
                .eq(deviceDTO.getType() != null, DeviceDO::getType, deviceDTO.getType())
                .last("limit 1");

            DeviceDO existingRecord = deviceService.getOne(queryWrapper);
            if (existingRecord == null) {
                log.warn("要删除的设备不存在");
                return false;
            }

            boolean success = deviceService.removeById(existingRecord.getId());
            if (success) {
                clearCache(existingRecord.getId(), existingRecord.getDeviceId(), null);
                return true;
            } else {
                throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, "数据库删除失败");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除设备失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：批量删除记录
     * 标准流程：校验参数 -> 转换DO条件 -> 查询匹配记录 -> 批量删除 -> 清理缓存
     * 批量实现：使用LambdaQueryWrapper构建批量删除条件
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteBatch(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "删除条件不能为空");

        try {
            LambdaQueryWrapper<DeviceDO> queryWrapper = new LambdaQueryWrapper<>();

            // 使用condition参数避免空值判断，只有非空时才添加条件
            queryWrapper.eq(deviceDTO.getId() != null, DeviceDO::getId, deviceDTO.getId())
                .eq(deviceDTO.getDeviceId() != null, DeviceDO::getDeviceId, deviceDTO.getDeviceId())
                .eq(deviceDTO.getStatus() != null, DeviceDO::getStatus, deviceDTO.getStatus())
                .eq(deviceDTO.getName() != null, DeviceDO::getName, deviceDTO.getName())
                .eq(deviceDTO.getIp() != null, DeviceDO::getIp, deviceDTO.getIp())
                .eq(deviceDTO.getPort() != null, DeviceDO::getPort, deviceDTO.getPort())
                .eq(deviceDTO.getServerIp() != null, DeviceDO::getServerIp, deviceDTO.getServerIp())
                .eq(deviceDTO.getType() != null, DeviceDO::getType, deviceDTO.getType());

            // 先查询要删除的记录，用于清理缓存
            List<DeviceDO> toDelete = deviceService.list(queryWrapper);
            if (toDelete.isEmpty()) {
                log.warn("没有找到要删除的记录");
                return false;
            }

            boolean success = deviceService.remove(queryWrapper);
            if (success) {
                // 清理每个删除记录的缓存
                for (DeviceDO deleted : toDelete) {
                    clearCache(deleted.getId(), deleted.getDeviceId(), null);
                }
                return true;
            } else {
                throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, "批量删除失败");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量删除设备失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, e.getMessage());
        }
    }

    /**
     * 模板方法：分页查询
     * 标准流程：校验参数 -> 转换DO条件 -> 分页查询数据库 -> 转换记录为DTO -> 返回Page<DTO>
     * 分页实现：使用LambdaQueryWrapper + 默认排序（创建时间降序）
     */
    public Page<DeviceDTO> getPage(DeviceDTO deviceDTO, int page, int size) {
        if (page < 1)
            throw new IllegalArgumentException("页码必须大于0");
        if (size < 1 || size > 1000)
            throw new IllegalArgumentException("页大小必须在1-1000之间");

        try {
            LambdaQueryWrapper<DeviceDO> queryWrapper = new LambdaQueryWrapper<>();

            // 使用condition参数避免空值判断，只有非空时才添加条件
            if (deviceDTO != null) {
                queryWrapper.eq(deviceDTO.getId() != null, DeviceDO::getId, deviceDTO.getId())
                    .eq(deviceDTO.getDeviceId() != null, DeviceDO::getDeviceId, deviceDTO.getDeviceId())
                    .eq(deviceDTO.getStatus() != null, DeviceDO::getStatus, deviceDTO.getStatus())
                    .like(deviceDTO.getName() != null, DeviceDO::getName, deviceDTO.getName())
                    .eq(deviceDTO.getIp() != null, DeviceDO::getIp, deviceDTO.getIp())
                    .eq(deviceDTO.getPort() != null, DeviceDO::getPort, deviceDTO.getPort())
                    .eq(deviceDTO.getServerIp() != null, DeviceDO::getServerIp, deviceDTO.getServerIp())
                    .eq(deviceDTO.getType() != null, DeviceDO::getType, deviceDTO.getType());
            }

            queryWrapper.orderByDesc(DeviceDO::getCreateTime);

            Page<DeviceDO> pageQuery = new Page<>(page, size);
            Page<DeviceDO> doPage = deviceService.page(pageQuery, queryWrapper);

            // 转换为DTO分页结果
            Page<DeviceDTO> dtoPage = new Page<>(page, size);
            dtoPage.setTotal(doPage.getTotal());
            dtoPage.setPages(doPage.getPages());
            dtoPage.setCurrent(doPage.getCurrent());
            dtoPage.setSize(doPage.getSize());

            List<DeviceDTO> dtoRecords = deviceAssembler.toDeviceDTOList(doPage.getRecords());
            dtoPage.setRecords(dtoRecords);

            return dtoPage;
        } catch (Exception e) {
            log.error("分页查询设备失败 - 错误: {}", e.getMessage(), e);
            throw new ServiceException(ServiceExceptionEnum.DEVICE_OPERATION_FAILED, e.getMessage());
        }
    }

    // ================================
    // Phase 2a：心跳定向更新 + 单调写
    // ================================

    /**
     * 心跳/在线态轻量定向更新（Phase 2a：修 P3 写放大 + H3 单调性）。
     * <p>
     * 不读整行、不整行 UPDATE、不连坐列表缓存。仅对 status / keepaliveTime 两列做单条定向 UPDATE，
     * 命中 device_id 唯一索引。keepaliveTime 走<b>单调条件</b>（仅当传入时间戳比库里新才更新），
     * 防 UDP/跨节点乱序回灌——这是跨节点漂移下的唯一正确性保证（红线 3）。
     * </p>
     * <p>
     * ⚠️ 单调条件只管 keepaliveTime 这种自愈字段；离线终态请走 {@link #patchOfflineTerminal(String)}，
     * 终态不可被"时间戳更旧"挡住。
     * </p>
     *
     * @param deviceId      设备国标 ID
     * @param status        在线态（{@code DeviceConstant.Status} ONLINE/OFFLINE），可空表示不更新
     * @param keepaliveTime 心跳时间戳，可空表示不更新；非空时受单调条件保护
     */
    public void patchLiveness(String deviceId, Integer status, LocalDateTime keepaliveTime) {
        Assert.hasText(deviceId, "设备ID不能为空");

        LambdaUpdateWrapper<DeviceDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(DeviceDO::getDeviceId, deviceId)
            .set(status != null, DeviceDO::getStatus, status)
            .set(keepaliveTime != null, DeviceDO::getKeepaliveTime, keepaliveTime)
            .set(DeviceDO::getUpdateTime, LocalDateTime.now())
            // 单调条件：仅当传入时间戳比库里新才更新（防 UDP/跨节点乱序回灌）
            .and(keepaliveTime != null, w -> w
                .isNull(DeviceDO::getKeepaliveTime)
                .or().lt(DeviceDO::getKeepaliveTime, keepaliveTime));

        boolean updated = deviceService.update(uw);
        if (updated) {
            // 仅精确 evict 该设备，不连坐列表缓存
            clearCache(null, deviceId, null);
        } else {
            log.debug("patchLiveness 未更新任何记录（设备不存在或被单调条件挡下）- deviceId: {}, keepaliveTime: {}",
                deviceId, keepaliveTime);
        }
    }

    /**
     * 设备活跃时间更新（带心跳合并优化，Phase 2a）。
     * <p>
     * 节点本地合并策略：
     * <ul>
     *   <li>首次心跳 → 立即写 DB + 记录 lastPersistTs</li>
     *   <li>30s 内的重复心跳 → 仅刷缓存，跳过 DB 写</li>
     *   <li>30s 后 → 走 DB 写 + 更新 lastPersistTs</li>
     * </ul>
     * <b>不影响最终一致性</b>：缓存刷新确保下次读到最新心跳时间，底层 patchLiveness() 单调条件保护漂移场景。
     * </p>
     *
     * @param deviceId      设备国标 ID
     * @param status        设备状态（可选，一般为 ONLINE）
     * @param keepaliveTime 心跳时间
     */
    public void patchLivenessWithCoalesce(String deviceId, Integer status, LocalDateTime keepaliveTime) {
        Assert.hasText(deviceId, "设备ID不能为空");
        Assert.notNull(keepaliveTime, "心跳时间不能为空");

        long now = System.currentTimeMillis();
        Long lastTs = lastPersistTs.getOrDefault(deviceId, 0L);

        // 判断是否在合并窗口内
        if (now - lastTs < coalesceWindowMs) {
            // 30s 内：仅刷缓存，不写 DB（加速）
            clearCache(null, deviceId, null);
            log.debug("心跳合并：{}ms 内，仅刷缓存 - deviceId: {}", now - lastTs, deviceId);
        } else {
            // 首次心跳或超出窗口：走 DB 写 + 更新 lastPersistTs
            patchLiveness(deviceId, status, keepaliveTime);
            lastPersistTs.put(deviceId, now);
            // 续期路由 TTL（开关关闭时跳过）
            if (deviceNodeRouteService != null) {
                deviceNodeRouteService.renewDevice(deviceId);
            }
            log.debug("心跳合并：已写 DB - deviceId: {}, keepaliveTime: {}", deviceId, keepaliveTime);
        }
    }

    /**
     * 清理心跳合并缓存（节点本地）。
     * <p>
     * 用于重启兜底：设备下线/注销时调用，避免旧的 lastPersistTs 残留。
     * </p>
     *
     * @param deviceId 设备国标 ID
     */
    public void clearCoalesceCache(String deviceId) {
        if (deviceId != null && lastPersistTs.remove(deviceId) != null) {
            log.debug("已清理心跳合并缓存 - deviceId: {}", deviceId);
        }
    }

    /**
     * 设备离线终态写入（Phase 2a：终态优先，无单调条件）。
     * <p>
     * 离线属强一致终态，<b>不加单调条件</b>——终态不可被"时间戳更旧"挡住。
     * 由设备主动注销 / Offline 事件触发时调用，强制写 OFFLINE 并精确 evict 缓存。
     * </p>
     *
     * @param deviceId 设备国标 ID
     */
    public void patchOfflineTerminal(String deviceId) {
        Assert.hasText(deviceId, "设备ID不能为空");

        LambdaUpdateWrapper<DeviceDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(DeviceDO::getDeviceId, deviceId)
            .set(DeviceDO::getStatus, DeviceConstant.Status.OFFLINE)
            .set(DeviceDO::getUpdateTime, LocalDateTime.now());

        boolean updated = deviceService.update(uw);
        if (updated) {
            clearCache(null, deviceId, null);
            // Phase 2a: 清理心跳合并缓存，避免设备重新上线时被误判为"窗口内重复"
            clearCoalesceCache(deviceId);
        } else {
            log.debug("patchOfflineTerminal 未更新任何记录（设备不存在）- deviceId: {}", deviceId);
        }
    }

    // ================================
    // 统一缓存清理模板方法
    // ================================

    /**
     * 统一缓存清理模板方法（Phase 1：精确 evict，去 cache.clear()）
     * <p>
     * 通过 {@link DeviceCacheKey} 统一 key 生成，与 {@code @Cacheable} 写入 key 一致，
     * 杜绝旧代码"裸 id + do:/dto: 前缀"三套 key 互不命中的脏读根因（P2），
     * 且不再 {@code cache.clear()} 连坐整个缓存区（P1）。列表缓存独立、靠短 TTL 自然失效。
     * </p>
     */
    private void clearCache(Long id, String oldDeviceId, String newDeviceId) {
        try {
            Optional.ofNullable(cacheManager.getCache(DeviceCacheKey.CACHE_NAME))
                .ifPresent(cache -> {
                    if (id != null) {
                        cache.evict(DeviceCacheKey.byId(id));
                        scheduleDelayedEvict(DeviceCacheKey.CACHE_NAME, DeviceCacheKey.byId(id));
                    }
                    if (oldDeviceId != null) {
                        cache.evict(DeviceCacheKey.byDeviceId(oldDeviceId));
                        scheduleDelayedEvict(DeviceCacheKey.CACHE_NAME, DeviceCacheKey.byDeviceId(oldDeviceId));
                    }
                    if (newDeviceId != null && !newDeviceId.equals(oldDeviceId)) {
                        cache.evict(DeviceCacheKey.byDeviceId(newDeviceId));
                        scheduleDelayedEvict(DeviceCacheKey.CACHE_NAME, DeviceCacheKey.byDeviceId(newDeviceId));
                    }
                });
        } catch (Exception e) {
            log.warn("缓存清理异常，但不影响业务流程: {}", e.getMessage());
        }
    }

    // ================================
    // Phase 1: 延迟双删
    // ================================

    /** 获取 DelayedCacheEviction（懒初始化，Redis 不可用时为 null）。 */
    private DelayedCacheEviction eviction() {
        if (delayedEviction == null && stringRedisTemplate != null) {
            delayedEviction = new DelayedCacheEviction(stringRedisTemplate, cacheManager);
        }
        return delayedEviction;
    }

    private void scheduleDelayedEvict(String cacheName, String key) {
        DelayedCacheEviction e = eviction();
        if (e != null) {
            e.scheduleEvict(cacheName, key);
        }
    }

    /**
     * 延迟双删扫描器：每 200ms 取出到期的 evict 任务并执行（修 A4）。
     * {@code @EnableScheduling} 已在 ApplicationWeb 上声明。
     */
    @Scheduled(fixedDelay = 200)
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public void drainDelayedEvictions() {
        DelayedCacheEviction e = eviction();
        if (e != null) {
            e.drainDue(System.currentTimeMillis());
        }
    }

    // ================================
    // Controller层兼容方法
    // ================================

    /**
     * 创建设备 - 兼容原Controller接口
     * 委托给标准的add方法
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(DeviceDTO deviceDTO) {
        return add(deviceDTO);
    }

    /**
     * 更新设备 - 兼容原Controller接口
     * 委托给标准的updateById方法
     */
    @Transactional(rollbackFor = Exception.class)
    public Long updateDevice(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "设备信息不能为空");
        Assert.notNull(deviceDTO.getId(), "设备ID不能为空");

        return updateById(deviceDTO.getId(), deviceDTO);
    }

    /**
     * 保存或更新设备 - 兼容原Controller接口
     * 智能判断是创建还是更新
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdate(DeviceDTO deviceDTO) {
        Assert.notNull(deviceDTO, "设备信息不能为空");
        Assert.hasText(deviceDTO.getDeviceId(), "设备ID不能为空");

        // 检查设备是否已存在
        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setDeviceId(deviceDTO.getDeviceId());
        DeviceDTO existingDevice = get(queryDTO);

        log.debug("saveOrUpdate - 检查设备是否存在: deviceId={}, existingDevice={}",
            deviceDTO.getDeviceId(), existingDevice != null ? existingDevice.getId() : "null");

        if (existingDevice != null) {
            // 设备存在，更新设备
            Long result = updateById(existingDevice.getId(), deviceDTO);
            log.debug("saveOrUpdate - 更新设备完成: deviceId={}, result={}",
                deviceDTO.getDeviceId(), result);
            return result;
        } else {
            // 设备不存在，创建新设备
            Long result = add(deviceDTO);
            log.debug("saveOrUpdate - 创建设备完成: deviceId={}, result={}",
                deviceDTO.getDeviceId(), result);
            return result;
        }
    }

    /**
     * 删除设备 - 兼容原Controller接口
     * 委托给标准的deleteOne方法
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDevice(String deviceId) {
        Assert.hasText(deviceId, "设备ID不能为空");

        DeviceDTO deleteDTO = new DeviceDTO();
        deleteDTO.setDeviceId(deviceId);
        return deleteOne(deleteDTO);
    }

    /**
     * 更新设备状态 - 兼容原Controller接口
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(String deviceId, int status) {
        Assert.hasText(deviceId, "设备ID不能为空");

        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setDeviceId(deviceId);
        DeviceDTO existingDevice = get(queryDTO);

        if (existingDevice == null) {
            throw new ServiceException(ServiceExceptionEnum.DEVICE_NOT_FOUND, "设备不存在: " + deviceId);
        }

        DeviceDTO updateDTO = new DeviceDTO();
        updateDTO.setStatus(status);

        updateById(existingDevice.getId(), updateDTO);
    }

    // ================================
    // 原有查询方法保持不变（向下兼容）
    // ================================

    public DeviceDO getByDeviceId(String deviceId) {
        Assert.hasText(deviceId, "设备ID不能为空");
        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setDeviceId(deviceId);
        DeviceDTO result = get(queryDTO);
        return result != null ? deviceAssembler.toDeviceDO(result) : null;
    }

    @Cacheable(value = DeviceCacheKey.CACHE_NAME, key = "T(io.github.lunasaw.voglander.manager.cache.DeviceCacheKey).byDeviceId(#deviceId)", unless = "#result == null")
    public DeviceDTO getDtoByDeviceId(String deviceId) {
        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setDeviceId(deviceId);
        return get(queryDTO);
    }

    public DeviceDTO getDeviceDTOById(Long id) {
        DeviceDTO queryDTO = new DeviceDTO();
        queryDTO.setId(id);
        return get(queryDTO);
    }

    public DeviceDTO getDeviceDTOByEntity(DeviceDO device) {
        DeviceDTO queryDTO = deviceAssembler.toDeviceDTO(device);
        return get(queryDTO);
    }

    public List<DeviceDTO> listDeviceDTO(DeviceDO device) {
        DeviceDTO queryDTO = device != null ? deviceAssembler.toDeviceDTO(device) : new DeviceDTO();
        Page<DeviceDTO> page = getPage(queryDTO, 1, Integer.MAX_VALUE);
        return page.getRecords();
    }

    public Page<DeviceDTO> pageQuerySimple(int page, int size) {
        return getPage(new DeviceDTO(), page, size);
    }

    public Page<DeviceDTO> pageQuery(int page, int size, QueryWrapper<DeviceDO> queryWrapper) {
        // 为了兼容性保留，但建议使用新的getPage方法
        return getPage(new DeviceDTO(), page, size);
    }

    public List<DeviceDO> list() {
        return deviceService.list();
    }

    // ================================
    // 批量操作兼容方法
    // ================================

    /**
     * 批量创建设备 - 兼容原Controller接口
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateDevice(List<DeviceDTO> deviceDTOList) {
        if (deviceDTOList == null || deviceDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (DeviceDTO deviceDTO : deviceDTOList) {
            try {
                add(deviceDTO);
                successCount++;
            } catch (Exception e) {
                log.error("批量创建设备失败，deviceId: {}, error: {}", deviceDTO.getDeviceId(), e.getMessage());
            }
        }

        log.info("批量创建设备完成，成功创建: {} 个设备，总计: {} 个", successCount, deviceDTOList.size());
        return successCount;
    }

    /**
     * 批量更新设备 - 兼容原Controller接口
     */
    public int batchUpdateDevice(List<DeviceDTO> deviceDTOList) {
        if (deviceDTOList == null || deviceDTOList.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (DeviceDTO deviceDTO : deviceDTOList) {
            try {
                if (deviceDTO.getId() != null) {
                    updateById(deviceDTO.getId(), deviceDTO);
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量更新设备失败，ID: {}, error: {}", deviceDTO.getId(), e.getMessage());
            }
        }

        return successCount;
    }
}