package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.manager.assembler.MediaSessionAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.service.MediaSessionService;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 媒体会话管理器
 * <p>
 * 负责 GB28181 INVITE 点播/回放会话的状态管理。基于标准模板方法（add/update/get/deleteOne/getPage）
 * 提供 CRUD，并暴露由 {@code VoglanderBusinessNotifier} 的 Session.* / Notify.MediaStatus
 * 事件驱动的业务方法（onInviteOk/onInviteFailure/onBye/onAck/onMediaStatus）。
 * </p>
 *
 * <p>
 * 设计要点：
 * </p>
 * <ul>
 * <li>会话业务主键为 {@code callId}（INVITE 阶段的 SIP Call-ID）。</li>
 * <li>所有查询使用 {@link LambdaQueryWrapper} 的 condition 参数避免空值判断。</li>
 * <li>对外接口参数/返回值均为 DTO，DO 转换通过 {@link MediaSessionAssembler}。</li>
 * </ul>
 *
 * @author luna
 * @since 2025-05-29
 */
@Slf4j
@Component
public class MediaSessionManager {

    /**
     * 缓存名称。
     */
    private static final String   CACHE_NAME = "mediaSession";

    @Autowired
    private MediaSessionService   mediaSessionService;

    @Autowired
    private MediaSessionAssembler mediaSessionAssembler;

    @Autowired
    private CacheManager          cacheManager;

    // ================================
    // 模板方法（CRUD）
    // ================================

    /**
     * 模板方法：新增会话。
     * 标准流程：校验参数 -> 转换DO -> 设置时间 -> 插入数据库 -> 清缓存 -> 返回ID。
     *
     * @param dto 会话DTO，callId 不能为空
     * @return 主键ID
     */
    public Long add(MediaSessionDTO dto) {
        Assert.notNull(dto, "会话信息不能为空");
        Assert.hasText(dto.getCallId(), "callId不能为空");

        MediaSessionDO mediaSessionDO = mediaSessionAssembler.dtoToDo(dto);
        LocalDateTime now = LocalDateTime.now();
        mediaSessionDO.setCreateTime(now);
        mediaSessionDO.setUpdateTime(now);

        boolean success = mediaSessionService.save(mediaSessionDO);
        if (!success) {
            throw new RuntimeException("媒体会话插入失败");
        }

        clearCache(mediaSessionDO.getId(), null, mediaSessionDO.getCallId());
        return mediaSessionDO.getId();
    }

    /**
     * 模板方法：条件更新（查询条件与更新内容分离）。
     *
     * @param queryDTO 查询条件
     * @param updateDTO 更新内容
     * @return 被更新记录的主键ID，未命中返回 null
     */
    public Long update(MediaSessionDTO queryDTO, MediaSessionDTO updateDTO) {
        Assert.notNull(queryDTO, "查询条件不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        MediaSessionDO existing = mediaSessionService.getOne(buildQuery(queryDTO).last("limit 1"));
        if (existing == null) {
            log.warn("更新媒体会话：未命中记录, query={}", queryDTO);
            return null;
        }

        return applyUpdate(existing, updateDTO);
    }

    /**
     * 扩展方法：通过ID更新指定字段。
     *
     * @param id 主键ID
     * @param updateDTO 更新内容
     * @return 主键ID
     */
    public Long updateById(Long id, MediaSessionDTO updateDTO) {
        Assert.notNull(id, "会话ID不能为空");
        Assert.notNull(updateDTO, "更新内容不能为空");

        MediaSessionDO existing = mediaSessionService.getById(id);
        if (existing == null) {
            throw new RuntimeException("未找到要更新的媒体会话: " + id);
        }
        return applyUpdate(existing, updateDTO);
    }

    /**
     * 模板方法：单条查询。
     *
     * @param dto 查询条件
     * @return 命中的会话DTO，未命中返回 null
     */
    public MediaSessionDTO get(MediaSessionDTO dto) {
        Assert.notNull(dto, "查询条件不能为空");

        MediaSessionDO existing = mediaSessionService.getOne(buildQuery(dto).last("limit 1"));
        return mediaSessionAssembler.doToDto(existing);
    }

    /**
     * 模板方法：删除单条记录。
     *
     * @param dto 删除条件
     * @return 是否删除成功
     */
    public Boolean deleteOne(MediaSessionDTO dto) {
        Assert.notNull(dto, "删除条件不能为空");

        MediaSessionDO existing = mediaSessionService.getOne(buildQuery(dto).last("limit 1"));
        if (existing == null) {
            return true;
        }
        boolean success = mediaSessionService.removeById(existing.getId());
        clearCache(existing.getId(), existing.getCallId(), null);
        return success;
    }

    /**
     * 模板方法：批量删除。
     *
     * @param dto 删除条件
     * @return 是否删除成功
     */
    public Boolean deleteBatch(MediaSessionDTO dto) {
        Assert.notNull(dto, "删除条件不能为空");
        return mediaSessionService.remove(buildQuery(dto));
    }

    /**
     * 模板方法：分页查询（默认按创建时间降序）。
     *
     * @param dto 查询条件
     * @param page 页码（从1开始）
     * @param size 页大小（1-1000）
     * @return 分页DTO结果
     */
    public Page<MediaSessionDTO> getPage(MediaSessionDTO dto, int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("页大小必须在1-1000之间");
        }

        LambdaQueryWrapper<MediaSessionDO> queryWrapper = buildQuery(dto)
            .orderByDesc(MediaSessionDO::getCreateTime);

        Page<MediaSessionDO> doPage = mediaSessionService.page(new Page<>(page, size), queryWrapper);

        Page<MediaSessionDTO> dtoPage = new Page<>(page, size);
        dtoPage.setTotal(doPage.getTotal());
        dtoPage.setPages(doPage.getPages());
        dtoPage.setCurrent(doPage.getCurrent());
        dtoPage.setSize(doPage.getSize());
        dtoPage.setRecords(mediaSessionAssembler.doListToDtoList(doPage.getRecords()));
        return dtoPage;
    }

    /**
     * 按 callId 查询会话。
     *
     * @param callId SIP Call-ID
     * @return 命中的会话DTO，未命中返回 null
     */
    public MediaSessionDTO getByCallId(String callId) {
        Assert.hasText(callId, "callId不能为空");
        MediaSessionDTO query = new MediaSessionDTO();
        query.setCallId(callId);
        return get(query);
    }

    // ================================
    // 业务事件方法（由 Notifier 驱动）
    // ================================

    /**
     * INVITE 200 OK：会话建立成功，状态置为 ACTIVE。
     * 若 callId 已有记录则更新状态，否则新建一条 ACTIVE 记录。
     *
     * @param callId SIP Call-ID
     * @param deviceId 设备ID
     * @return 会话主键ID
     */
    public Long onInviteOk(String callId, String deviceId) {
        Assert.hasText(callId, "callId不能为空");

        MediaSessionDTO existing = getByCallId(callId);
        if (existing != null) {
            MediaSessionDTO update = new MediaSessionDTO();
            update.setStatus(MediaSessionConstant.Status.ACTIVE);
            if (deviceId != null) {
                update.setDeviceId(deviceId);
            }
            return updateById(existing.getId(), update);
        }

        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setCallId(callId);
        dto.setDeviceId(deviceId);
        dto.setStatus(MediaSessionConstant.Status.ACTIVE);
        return add(dto);
    }

    /**
     * INVITE 失败：状态置为 FAILED。
     *
     * @param callId SIP Call-ID
     * @param statusCode SIP 失败状态码
     * @return 会话主键ID，无对应记录时新建
     */
    public Long onInviteFailure(String callId, int statusCode) {
        Assert.hasText(callId, "callId不能为空");
        log.info("媒体会话 INVITE 失败, callId={}, statusCode={}", callId, statusCode);

        MediaSessionDTO existing = getByCallId(callId);
        if (existing != null) {
            MediaSessionDTO update = new MediaSessionDTO();
            update.setStatus(MediaSessionConstant.Status.FAILED);
            return updateById(existing.getId(), update);
        }

        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setCallId(callId);
        dto.setStatus(MediaSessionConstant.Status.FAILED);
        return add(dto);
    }

    /**
     * BYE：会话关闭，将该设备的活跃会话状态置为 CLOSED。
     *
     * @param deviceId 设备ID
     * @return 受影响的记录数
     */
    public int onBye(String deviceId) {
        Assert.hasText(deviceId, "deviceId不能为空");
        log.info("媒体会话 BYE, deviceId={}", deviceId);

        LambdaQueryWrapper<MediaSessionDO> queryWrapper = new LambdaQueryWrapper<MediaSessionDO>()
            .eq(MediaSessionDO::getDeviceId, deviceId)
            .ne(MediaSessionDO::getStatus, MediaSessionConstant.Status.CLOSED);

        MediaSessionDO update = new MediaSessionDO();
        update.setStatus(MediaSessionConstant.Status.CLOSED);
        update.setUpdateTime(LocalDateTime.now());

        boolean success = mediaSessionService.update(update, queryWrapper);
        clearCache(null, null, null);
        return success ? 1 : 0;
    }

    /**
     * ACK：INVITE 三向握手完成确认，确保会话为 ACTIVE。
     *
     * @param callId SIP Call-ID
     * @return 会话主键ID，无对应记录时返回 null
     */
    public Long onAck(String callId) {
        Assert.hasText(callId, "callId不能为空");
        log.debug("媒体会话 ACK, callId={}", callId);

        MediaSessionDTO existing = getByCallId(callId);
        if (existing == null) {
            log.warn("媒体会话 ACK：未找到会话, callId={}", callId);
            return null;
        }
        if (MediaSessionConstant.Status.ACTIVE == existing.getStatus()) {
            return existing.getId();
        }
        MediaSessionDTO update = new MediaSessionDTO();
        update.setStatus(MediaSessionConstant.Status.ACTIVE);
        return updateById(existing.getId(), update);
    }

    /**
     * MediaStatus 通知：媒体流状态变化（如设备侧 NOTIFY 媒体结束）。
     * 当通知表示媒体结束时，关闭该设备的活跃会话。
     *
     * @param deviceId 设备ID
     * @param notifyType 通知类型（如 "121" 表示历史媒体发送结束）
     * @return 受影响的记录数
     */
    public int onMediaStatus(String deviceId, String notifyType) {
        Assert.hasText(deviceId, "deviceId不能为空");
        log.info("媒体会话状态通知, deviceId={}, notifyType={}", deviceId, notifyType);
        // 媒体发送结束（GB28181 NotifyType=121）等价于会话结束
        return onBye(deviceId);
    }

    // ================================
    // 私有辅助
    // ================================

    /**
     * 应用更新内容到已存在记录并落库。
     */
    private Long applyUpdate(MediaSessionDO existing, MediaSessionDTO updateDTO) {
        if (updateDTO.getDeviceId() != null) {
            existing.setDeviceId(updateDTO.getDeviceId());
        }
        if (updateDTO.getChannelId() != null) {
            existing.setChannelId(updateDTO.getChannelId());
        }
        if (updateDTO.getSsrc() != null) {
            existing.setSsrc(updateDTO.getSsrc());
        }
        if (updateDTO.getStream() != null) {
            existing.setStream(updateDTO.getStream());
        }
        if (updateDTO.getStatus() != null) {
            existing.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getSessionType() != null) {
            existing.setSessionType(updateDTO.getSessionType());
        }
        if (updateDTO.getExtend() != null) {
            existing.setExtend(updateDTO.getExtend());
        }
        existing.setUpdateTime(LocalDateTime.now());

        boolean success = mediaSessionService.updateById(existing);
        if (!success) {
            throw new RuntimeException("媒体会话更新失败: id=" + existing.getId());
        }
        clearCache(existing.getId(), existing.getCallId(), null);
        return existing.getId();
    }

    /**
     * 基于 DTO 构建查询条件（使用 condition 参数避免空值判断）。
     */
    private LambdaQueryWrapper<MediaSessionDO> buildQuery(MediaSessionDTO dto) {
        LambdaQueryWrapper<MediaSessionDO> queryWrapper = new LambdaQueryWrapper<>();
        if (dto == null) {
            return queryWrapper;
        }
        return queryWrapper
            .eq(dto.getId() != null, MediaSessionDO::getId, dto.getId())
            .eq(dto.getCallId() != null, MediaSessionDO::getCallId, dto.getCallId())
            .eq(dto.getDeviceId() != null, MediaSessionDO::getDeviceId, dto.getDeviceId())
            .eq(dto.getChannelId() != null, MediaSessionDO::getChannelId, dto.getChannelId())
            .eq(dto.getSsrc() != null, MediaSessionDO::getSsrc, dto.getSsrc())
            .eq(dto.getStream() != null, MediaSessionDO::getStream, dto.getStream())
            .eq(dto.getStatus() != null, MediaSessionDO::getStatus, dto.getStatus())
            .eq(dto.getSessionType() != null, MediaSessionDO::getSessionType, dto.getSessionType());
    }

    /**
     * 模板方法：统一缓存清理。
     *
     * @param id 主键ID
     * @param oldKey 旧业务键 callId（可能为空）
     * @param newKey 新业务键 callId（可能为空）
     */
    private void clearCache(Long id, String oldKey, String newKey) {
        try {
            if (id != null) {
                Optional.ofNullable(cacheManager.getCache(CACHE_NAME))
                    .ifPresent(cache -> cache.evict(id));
            }
            if (oldKey != null) {
                Optional.ofNullable(cacheManager.getCache(CACHE_NAME))
                    .ifPresent(cache -> cache.evict("key:" + oldKey));
            }
            if (newKey != null && !newKey.equals(oldKey)) {
                Optional.ofNullable(cacheManager.getCache(CACHE_NAME))
                    .ifPresent(cache -> cache.evict("key:" + newKey));
            }
        } catch (Exception e) {
            log.warn("缓存清理异常，但不影响业务流程: {}", e.getMessage());
        }
    }
}
