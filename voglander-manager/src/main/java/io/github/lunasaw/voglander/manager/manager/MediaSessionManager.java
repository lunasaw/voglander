package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
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
            throw new ServiceException(ServiceExceptionEnum.MEDIA_SESSION_OPERATION_FAILED, "媒体会话插入失败");
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
            throw new ServiceException(ServiceExceptionEnum.MEDIA_SESSION_OPERATION_FAILED, "未找到要更新的媒体会话: " + id);
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

    /**
     * 按 streamId 查询会话。
     *
     * @param streamId 前端稳定主键
     * @return 命中的会话DTO，未命中返回 null
     */
    public MediaSessionDTO getByStreamId(String streamId) {
        Assert.hasText(streamId, "streamId不能为空");
        MediaSessionDTO query = new MediaSessionDTO();
        query.setStreamId(streamId);
        return get(query);
    }

    /**
     * 发起即刻回填：把 streamId 对应占位行的 callId 替换为<strong>真实 SIP Call-ID</strong>。
     *
     * <p>{@code startLive} 预写占位行时 callId 暂用 streamId（INVITE 尚未发出、真实 Call-ID 未知）。
     * INVITE 发出后框架同步返回真实 Call-ID，此处据此回填，使关流时能按真实 Call-ID 发 BYE 终止 dialog，
     * 不再依赖异步 {@code Session.InviteOk}（其携带的寻址 ID 在标准通道寻址下为 channelId，无法直接作设备主键）。
     *
     * <p>幂等：未找到占位行、或 callId 已等于 realCallId 时直接返回（不抛异常，不阻断首播主流程）。
     * 真实 Call-ID 由本次 INVITE 新生成，表中不存在，回填不会触发 call_id UNIQUE 冲突。
     *
     * @param streamId   流ID（占位行业务键）
     * @param realCallId INVITE 返回的真实 SIP Call-ID
     * @return 回填命中的会话主键ID；未命中返回 null
     */
    public Long backfillCallIdByStreamId(String streamId, String realCallId) {
        Assert.hasText(streamId, "streamId不能为空");
        Assert.hasText(realCallId, "realCallId不能为空");

        MediaSessionDTO existing = getByStreamId(streamId);
        if (existing == null) {
            log.warn("回填 callId 未找到占位行, streamId={}", streamId);
            return null;
        }
        if (realCallId.equals(existing.getCallId())) {
            return existing.getId();
        }
        MediaSessionDTO update = new MediaSessionDTO();
        update.setCallId(realCallId);
        Long id = updateById(existing.getId(), update);
        log.info("回填真实 callId 成功, streamId={}, realCallId={}, sessionId={}", streamId, realCallId, id);
        return id;
    }

    /**
     * 查询所有 ACTIVE 会话（GC 对账用）。
     *
     * @return ACTIVE 会话DTO列表
     */
    public java.util.List<MediaSessionDTO> getActiveSessions() {
        LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<MediaSessionDO>()
            .eq(MediaSessionDO::getStatus, MediaSessionConstant.Status.ACTIVE);
        return mediaSessionAssembler.doListToDtoList(mediaSessionService.list(qw));
    }

    /**
     * 查询某设备的所有 ACTIVE 会话。
     *
     * @param deviceId 设备ID
     * @return ACTIVE 会话DTO列表
     */
    public java.util.List<MediaSessionDTO> getActiveSessionsByDevice(String deviceId) {
        Assert.hasText(deviceId, "deviceId不能为空");
        LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<MediaSessionDO>()
            .eq(MediaSessionDO::getDeviceId, deviceId)
            .eq(MediaSessionDO::getStatus, MediaSessionConstant.Status.ACTIVE);
        return mediaSessionAssembler.doListToDtoList(mediaSessionService.list(qw));
    }

    /**
     * 查询指定媒体节点上的所有 ACTIVE 会话（节点故障流迁移用）。
     *
     * @param nodeServerId 媒体节点 serverId
     * @return ACTIVE 会话DTO列表
     */
    public java.util.List<MediaSessionDTO> getActiveSessionsByNode(String nodeServerId) {
        Assert.hasText(nodeServerId, "nodeServerId不能为空");
        LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<MediaSessionDO>()
            .eq(MediaSessionDO::getNodeServerId, nodeServerId)
            .eq(MediaSessionDO::getStatus, MediaSessionConstant.Status.ACTIVE);
        return mediaSessionAssembler.doListToDtoList(mediaSessionService.list(qw));
    }

    /**
     * 将早于 deadline 仍处于 INVITING 的占位会话批量标记为 FAILED（僵尸 GC）。
     *
     * @param deadline 截止时间，createTime 早于此值的 INVITING 行被标记
     * @return 受影响记录数
     */
    public int markTimeoutInvitingAsFailed(LocalDateTime deadline) {
        Assert.notNull(deadline, "deadline不能为空");
        LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<MediaSessionDO>()
            .eq(MediaSessionDO::getStatus, MediaSessionConstant.Status.INVITING)
            .lt(MediaSessionDO::getCreateTime, deadline);

        MediaSessionDO update = new MediaSessionDO();
        update.setStatus(MediaSessionConstant.Status.FAILED);
        update.setUpdateTime(LocalDateTime.now());

        boolean success = mediaSessionService.update(update, qw);
        clearCache(null, null, null);
        return success ? 1 : 0;
    }

    /**
     * 强制关闭指定会话（GC 对账：ZLM 无流时）。
     *
     * @param id 会话主键ID
     * @return 会话主键ID
     */
    public Long forceClose(Long id) {
        Assert.notNull(id, "会话ID不能为空");
        MediaSessionDTO update = new MediaSessionDTO();
        update.setStatus(MediaSessionConstant.Status.CLOSED);
        return updateById(id, update);
    }

    // ================================
    // 业务事件方法（由 Notifier 驱动）
    // ================================

    /**
     * INVITE 200 OK（callId 关联，推荐）：按 SIP Call-ID 找会话并置 ACTIVE。
     * <p>
     * 标准通道寻址下 {@code Session.InviteOk} 事件的 deviceId 字段被 To 头回显污染为 channelId，不可信；
     * 而 callId 是 SIP 会话唯一标识，且 startLive 发起时已由 {@code backfillCallIdByStreamId} 把真实 callId
     * 回填到占位行，故按 callId 必能命中。命中则置 ACTIVE；未命中（异常顺序/丢回填）退回三参兜底（新建 ACTIVE 行）。
     * </p>
     *
     * @param callId SIP Call-ID
     * @return 会话主键ID；未命中返回 null
     */
    public Long onInviteOk(String callId) {
        Assert.hasText(callId, "callId不能为空");
        MediaSessionDTO existing = getByCallId(callId);
        if (existing == null) {
            log.warn("媒体会话 InviteOk：按 callId 未找到会话（占位行回填可能丢失）, callId={}", callId);
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
     * INVITE 200 OK：会话建立成功，状态置为 ACTIVE（兼容两参重载，channelId 传 null）。
     *
     * @param callId SIP Call-ID
     * @param deviceId 设备ID
     * @return 会话主键ID
     */
    public Long onInviteOk(String callId, String deviceId) {
        return onInviteOk(callId, deviceId, null);
    }

    /**
     * INVITE 200 OK：会话建立成功，状态置为 ACTIVE。
     * <p>
     * 关联策略：
     * </p>
     * <ol>
     * <li>先按 callId 找行（正常路径 / 重复 OK 场景）。</li>
     * <li>找不到时，用 (deviceId, channelId, status=INVITING) 匹配 startLive 预写的占位行，回填 callId 并置 ACTIVE。</li>
     * <li>均未命中：创建新行（兜底，保持原有行为，含跨节点 UNIQUE 并发兜底）。</li>
     * </ol>
     *
     * @param callId SIP Call-ID
     * @param deviceId 设备ID
     * @param channelId 通道ID（用于关��� startLive 预写的 INVITING 占位行，可空）
     * @return 会话主键ID
     */
    public Long onInviteOk(String callId, String deviceId, String channelId) {
        Assert.hasText(callId, "callId不能为空");

        // 1. 先按 callId 找（正常路径 / 重复 OK 场景）
        MediaSessionDTO existing = getByCallId(callId);
        if (existing != null) {
            MediaSessionDTO update = new MediaSessionDTO();
            update.setStatus(MediaSessionConstant.Status.ACTIVE);
            if (deviceId != null) {
                update.setDeviceId(deviceId);
            }
            return updateById(existing.getId(), update);
        }

        // 2. 按占位行关联（startLive 预写 INVITING 行，callId 尚未回填）
        if (deviceId != null && channelId != null) {
            LambdaQueryWrapper<MediaSessionDO> qw = new LambdaQueryWrapper<>();
            qw.eq(MediaSessionDO::getDeviceId, deviceId)
                .eq(MediaSessionDO::getChannelId, channelId)
                .eq(MediaSessionDO::getStatus, MediaSessionConstant.Status.INVITING)
                .orderByDesc(MediaSessionDO::getCreateTime)
                .last("LIMIT 1");
            MediaSessionDO placeholder = mediaSessionService.getOne(qw);
            if (placeholder != null) {
                MediaSessionDTO update = new MediaSessionDTO();
                update.setCallId(callId);
                update.setStatus(MediaSessionConstant.Status.ACTIVE);
                update.setDeviceId(deviceId);
                return updateById(placeholder.getId(), update);
            }
        }

        // 3. 均未找到：创建新行（兜底，保持原有行为）
        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setCallId(callId);
        dto.setDeviceId(deviceId);
        dto.setChannelId(channelId);
        dto.setStatus(MediaSessionConstant.Status.ACTIVE);
        // Phase 4 / B3：先查后插在跨节点并发下会撞 call_id UNIQUE；捕获 DuplicateKey 转更新兜底（M1）。
        return insertOrUpdateOnDuplicate(dto, MediaSessionConstant.Status.ACTIVE, deviceId);
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
        // Phase 4 / B3：跨节点并发 upsert 同 callId 时撞 UNIQUE → 捕获 DuplicateKey 转更新兜底（M1）。
        return insertOrUpdateOnDuplicate(dto, MediaSessionConstant.Status.FAILED, null);
    }

    /**
     * 插入会话，命中 {@code call_id} UNIQUE（跨节点并发）时转为按 callId 更新状态兜底（Phase 4 / B3 / M1）。
     * <p>
     * 不依赖框架单节点 {@code invite-idempotency-window-ms}（那是节点本地 Caffeine 去重），
     * 而是用 DB 层 UNIQUE + {@code DuplicateKeyException} 捕获实现真正的跨节点幂等。
     * </p>
     *
     * @param dto      待插入会话（含 callId/status[/deviceId]）
     * @param status   冲突转更新时要写入的状态
     * @param deviceId 冲突转更新时要写入的 deviceId（可空表示不更新该列）
     * @return 会话主键ID
     */
    private Long insertOrUpdateOnDuplicate(MediaSessionDTO dto, int status, String deviceId) {
        try {
            return add(dto);
        } catch (Exception e) {
            // 检查是否为 UNIQUE 冲突：Spring DuplicateKeyException（MySQL）或 SQLite UNIQUE constraint
            Throwable cause = e;
            boolean isDuplicate = false;
            while (cause != null && !isDuplicate) {
                if (cause instanceof DuplicateKeyException) {
                    isDuplicate = true;
                } else {
                    String msg = cause.getMessage();
                    if (msg != null && (msg.contains("UNIQUE constraint failed") || msg.contains("Duplicate entry"))) {
                        isDuplicate = true;
                    }
                }
                cause = cause.getCause();
            }

            if (isDuplicate) {
                log.warn("媒体会话 callId 并发插入命中 UNIQUE，转更新兜底 - callId={}", dto.getCallId());
                MediaSessionDTO cur = getByCallId(dto.getCallId());
                if (cur != null) {
                    MediaSessionDTO update = new MediaSessionDTO();
                    update.setStatus(status);
                    if (deviceId != null) {
                        update.setDeviceId(deviceId);
                    }
                    return updateById(cur.getId(), update);
                }
            }
            // 非 UNIQUE 冲突或兜底失败 → 原样抛出
            throw e;
        }
    }

    /**
     * BYE：会话关闭，将匹配 SIP ID 的活跃会话状态置为 CLOSED。
     * <p>
     * {@code sipId} 为 BYE/MediaStatus 事件 From 头携带的 ID。标准通道寻址下，设备在 dialog 内发的 BYE
     * From 头是 channelId（原 INVITE 的 To）；而设备级会话该 ID 是 deviceId。框架这两类事件均不带 callId，
     * 故按 {@code device_id = sipId OR channel_id = sipId} 匹配，做到<strong>寻址无关</strong>，两种情形都能正确关流。
     * </p>
     *
     * @param sipId BYE 来源 SIP ID（可能是 deviceId 或 channelId）
     * @return 受影响的记录数
     */
    public int onBye(String sipId) {
        Assert.hasText(sipId, "sipId不能为空");
        log.info("媒体会话 BYE, sipId={}", sipId);

        LambdaQueryWrapper<MediaSessionDO> queryWrapper = new LambdaQueryWrapper<MediaSessionDO>()
            .and(w -> w.eq(MediaSessionDO::getDeviceId, sipId).or().eq(MediaSessionDO::getChannelId, sipId))
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
        if (updateDTO.getCallId() != null) {
            existing.setCallId(updateDTO.getCallId());
        }
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
        if (updateDTO.getStreamId() != null) {
            existing.setStreamId(updateDTO.getStreamId());
        }
        if (updateDTO.getNodeServerId() != null) {
            existing.setNodeServerId(updateDTO.getNodeServerId());
        }
        if (updateDTO.getRefCount() != null) {
            existing.setRefCount(updateDTO.getRefCount());
        }
        existing.setUpdateTime(LocalDateTime.now());

        boolean success = mediaSessionService.updateById(existing);
        if (!success) {
            throw new ServiceException(ServiceExceptionEnum.MEDIA_SESSION_OPERATION_FAILED, "媒体会话更新失败: id=" + existing.getId());
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
            .eq(dto.getSessionType() != null, MediaSessionDO::getSessionType, dto.getSessionType())
            .eq(dto.getStreamId() != null, MediaSessionDO::getStreamId, dto.getStreamId())
            .eq(dto.getNodeServerId() != null, MediaSessionDO::getNodeServerId, dto.getNodeServerId());
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
