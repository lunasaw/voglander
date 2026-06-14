package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luna.common.check.Assert;

import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceSubscriptionDTO;
import io.github.lunasaw.voglander.manager.service.DeviceSubscriptionService;
import io.github.lunasaw.voglander.repository.entity.DeviceSubscriptionDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备订阅状态管理器（GB28181-2022 §9.11）。
 * <p>
 * 遵循 Manager 模板方法：对外用 DTO，内部用 DO（private）。同时承载「意图」与「运行态」的回填。
 * UNIQUE(device_id, sub_type) 约束前先按业务键查现有记录（CLAUDE.md 规范），避免唯一约束冲突。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class DeviceSubscriptionManager {

    @Autowired
    private DeviceSubscriptionService subscriptionService;

    // ================================
    // 核心模板方法
    // ================================

    /**
     * 模板方法：新增（依赖数据库字段默认值）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long add(DeviceSubscriptionDTO dto) {
        Assert.notNull(dto, "订阅信息不能为空");
        Assert.hasText(dto.getDeviceId(), "设备ID不能为空");
        Assert.hasText(dto.getSubType(), "订阅类型不能为空");

        DeviceSubscriptionDO src = DeviceSubscriptionDTO.convertDO(dto);
        LocalDateTime now = LocalDateTime.now();
        src.setCreateTime(now);
        src.setUpdateTime(now);
        subscriptionService.save(src);
        return src.getId();
    }

    /**
     * 模板方法：按 ID 更新指定字段。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long updateById(Long id, DeviceSubscriptionDTO updateDTO) {
        Assert.notNull(id, "订阅ID不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        DeviceSubscriptionDO src = DeviceSubscriptionDTO.convertDO(updateDTO);
        src.setId(id);
        src.setUpdateTime(LocalDateTime.now());
        subscriptionService.updateById(src);
        return id;
    }

    /**
     * 模板方法：单条查询。
     */
    public DeviceSubscriptionDTO get(DeviceSubscriptionDTO query) {
        Assert.notNull(query, "查询条件不能为空");
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(query.getId() != null, DeviceSubscriptionDO::getId, query.getId())
            .eq(query.getDeviceId() != null, DeviceSubscriptionDO::getDeviceId, query.getDeviceId())
            .eq(query.getSubType() != null, DeviceSubscriptionDO::getSubType, query.getSubType())
            .eq(query.getStatus() != null, DeviceSubscriptionDO::getStatus, query.getStatus())
            .last("limit 1");
        return DeviceSubscriptionDTO.convertDTO(subscriptionService.getOne(qw));
    }

    /**
     * 模板方法：删除单条。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteOne(DeviceSubscriptionDTO query) {
        Assert.notNull(query, "删除条件不能为空");
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(query.getId() != null, DeviceSubscriptionDO::getId, query.getId())
            .eq(query.getDeviceId() != null, DeviceSubscriptionDO::getDeviceId, query.getDeviceId())
            .eq(query.getSubType() != null, DeviceSubscriptionDO::getSubType, query.getSubType());
        return subscriptionService.remove(qw);
    }

    /**
     * 模板方法：分页查询（默认按 createTime 降序）。
     */
    public Page<DeviceSubscriptionDTO> getPage(DeviceSubscriptionDTO query, int page, int size) {
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(query != null && query.getDeviceId() != null, DeviceSubscriptionDO::getDeviceId,
            query == null ? null : query.getDeviceId())
            .eq(query != null && query.getSubType() != null, DeviceSubscriptionDO::getSubType,
                query == null ? null : query.getSubType())
            .eq(query != null && query.getStatus() != null, DeviceSubscriptionDO::getStatus,
                query == null ? null : query.getStatus())
            .orderByDesc(DeviceSubscriptionDO::getCreateTime);

        Page<DeviceSubscriptionDO> doPage = subscriptionService.page(new Page<>(page, size), qw);
        Page<DeviceSubscriptionDTO> dtoPage = new Page<>(page, size);
        dtoPage.setTotal(doPage.getTotal());
        dtoPage.setPages(doPage.getPages());
        dtoPage.setCurrent(doPage.getCurrent());
        dtoPage.setSize(doPage.getSize());
        dtoPage.setRecords(doPage.getRecords().stream().map(DeviceSubscriptionDTO::convertDTO).collect(Collectors.toList()));
        return dtoPage;
    }

    // ================================
    // 业务方法
    // ================================

    /**
     * 按 device+type 取（UNIQUE）。
     */
    public DeviceSubscriptionDTO getByDeviceAndType(String deviceId, SubscriptionConstant.Type type) {
        if (deviceId == null || type == null) {
            return null;
        }
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(DeviceSubscriptionDO::getDeviceId, deviceId)
            .eq(DeviceSubscriptionDO::getSubType, type.name())
            .last("limit 1");
        return DeviceSubscriptionDTO.convertDTO(subscriptionService.getOne(qw));
    }

    /**
     * 该设备所有订阅行。
     */
    public List<DeviceSubscriptionDTO> listByDevice(String deviceId) {
        if (deviceId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(DeviceSubscriptionDO::getDeviceId, deviceId);
        return subscriptionService.list(qw).stream().map(DeviceSubscriptionDTO::convertDTO).collect(Collectors.toList());
    }

    /**
     * 一批设备的所有订阅行（列表回显批量填充，避免 N+1）。
     */
    public List<DeviceSubscriptionDTO> listByDeviceIds(List<String> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.in(DeviceSubscriptionDO::getDeviceId, deviceIds);
        return subscriptionService.list(qw).stream().map(DeviceSubscriptionDTO::convertDTO).collect(Collectors.toList());
    }

    /**
     * 意图开启的订阅（重订阅用）。
     */
    public List<DeviceSubscriptionDTO> listEnabledByDevice(String deviceId) {
        if (deviceId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(DeviceSubscriptionDO::getDeviceId, deviceId)
            .eq(DeviceSubscriptionDO::getEnabled, 1);
        return subscriptionService.list(qw).stream().map(DeviceSubscriptionDTO::convertDTO).collect(Collectors.toList());
    }

    /**
     * 即将过期的 ACTIVE 订阅（续订用）。
     *
     * @param before 过期时间早于该值的视为即将过期
     */
    public List<DeviceSubscriptionDTO> listExpiring(LocalDateTime before) {
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(DeviceSubscriptionDO::getStatus, SubscriptionConstant.Status.ACTIVE)
            .eq(DeviceSubscriptionDO::getEnabled, 1)
            .isNotNull(DeviceSubscriptionDO::getExpireTime)
            .le(DeviceSubscriptionDO::getExpireTime, before);
        return subscriptionService.list(qw).stream().map(DeviceSubscriptionDTO::convertDTO).collect(Collectors.toList());
    }

    /**
     * upsert 意图：UNIQUE(device,type) 约束前先查（CLAUDE.md 规范）。
     *
     * @return 该订阅行主键 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long upsertIntent(String deviceId, SubscriptionConstant.Type type, boolean enabled) {
        Assert.hasText(deviceId, "设备ID不能为空");
        Assert.notNull(type, "订阅类型不能为空");

        DeviceSubscriptionDO exist = getDoByDeviceAndType(deviceId, type);
        LocalDateTime now = LocalDateTime.now();
        if (exist != null) {
            LambdaUpdateWrapper<DeviceSubscriptionDO> uw = new LambdaUpdateWrapper<>();
            uw.eq(DeviceSubscriptionDO::getId, exist.getId())
                .set(DeviceSubscriptionDO::getEnabled, enabled ? 1 : 0)
                .set(DeviceSubscriptionDO::getUpdateTime, now);
            subscriptionService.update(null, uw);
            return exist.getId();
        }
        DeviceSubscriptionDO src = new DeviceSubscriptionDO();
        src.setDeviceId(deviceId);
        src.setSubType(type.name());
        src.setEnabled(enabled ? 1 : 0);
        src.setStatus(SubscriptionConstant.Status.INACTIVE);
        src.setCreateTime(now);
        src.setUpdateTime(now);
        subscriptionService.save(src);
        return src.getId();
    }

    /**
     * 回填运行态为 ACTIVE：callId + expireTime + status。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markActive(String deviceId, SubscriptionConstant.Type type, String callId, int expires) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<DeviceSubscriptionDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(DeviceSubscriptionDO::getDeviceId, deviceId)
            .eq(DeviceSubscriptionDO::getSubType, type.name())
            .set(DeviceSubscriptionDO::getStatus, SubscriptionConstant.Status.ACTIVE)
            .set(DeviceSubscriptionDO::getCallId, callId)
            .set(DeviceSubscriptionDO::getExpires, expires)
            .set(DeviceSubscriptionDO::getExpireTime, now.plusSeconds(expires))
            .set(DeviceSubscriptionDO::getUpdateTime, now);
        subscriptionService.update(null, uw);
    }

    /**
     * 回填运行态为 PENDING（设备离线，等注册钩子补发）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markPending(String deviceId, SubscriptionConstant.Type type) {
        updateStatus(deviceId, type, SubscriptionConstant.Status.PENDING);
    }

    /**
     * 回填运行态为 FAILED。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(String deviceId, SubscriptionConstant.Type type) {
        updateStatus(deviceId, type, SubscriptionConstant.Status.FAILED);
    }

    /**
     * 回填运行态为 INACTIVE，并清 callId。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markInactive(String deviceId, SubscriptionConstant.Type type) {
        LambdaUpdateWrapper<DeviceSubscriptionDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(DeviceSubscriptionDO::getDeviceId, deviceId)
            .eq(DeviceSubscriptionDO::getSubType, type.name())
            .set(DeviceSubscriptionDO::getStatus, SubscriptionConstant.Status.INACTIVE)
            .set(DeviceSubscriptionDO::getCallId, null)
            .set(DeviceSubscriptionDO::getExpireTime, null)
            .set(DeviceSubscriptionDO::getUpdateTime, LocalDateTime.now());
        subscriptionService.update(null, uw);
    }

    /**
     * 轻量更新 last_notify_time（收到该类通知时调用），失败不影响主流程。
     */
    public void touchLastNotify(String deviceId, SubscriptionConstant.Type type, LocalDateTime when) {
        if (deviceId == null || type == null) {
            return;
        }
        try {
            LambdaUpdateWrapper<DeviceSubscriptionDO> uw = new LambdaUpdateWrapper<>();
            uw.eq(DeviceSubscriptionDO::getDeviceId, deviceId)
                .eq(DeviceSubscriptionDO::getSubType, type.name())
                .set(DeviceSubscriptionDO::getLastNotifyTime, when != null ? when : LocalDateTime.now())
                .set(DeviceSubscriptionDO::getUpdateTime, LocalDateTime.now());
            subscriptionService.update(null, uw);
        } catch (Exception e) {
            log.warn("更新订阅通知时间失败, deviceId={}, type={}, 错误={}", deviceId, type, e.getMessage());
        }
    }

    // ================================
    // 私有工具
    // ================================

    private DeviceSubscriptionDO getDoByDeviceAndType(String deviceId, SubscriptionConstant.Type type) {
        LambdaQueryWrapper<DeviceSubscriptionDO> qw = new LambdaQueryWrapper<>();
        qw.eq(DeviceSubscriptionDO::getDeviceId, deviceId)
            .eq(DeviceSubscriptionDO::getSubType, type.name())
            .last("limit 1");
        return subscriptionService.getOne(qw);
    }

    private void updateStatus(String deviceId, SubscriptionConstant.Type type, int status) {
        LambdaUpdateWrapper<DeviceSubscriptionDO> uw = new LambdaUpdateWrapper<>();
        uw.eq(DeviceSubscriptionDO::getDeviceId, deviceId)
            .eq(DeviceSubscriptionDO::getSubType, type.name())
            .set(DeviceSubscriptionDO::getStatus, status)
            .set(DeviceSubscriptionDO::getUpdateTime, LocalDateTime.now());
        subscriptionService.update(null, uw);
    }
}
