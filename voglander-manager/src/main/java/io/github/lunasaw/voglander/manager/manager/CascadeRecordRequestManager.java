package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.manager.assembler.CascadeAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeRecordRequestDTO;
import io.github.lunasaw.voglander.manager.service.CascadeRecordRequestService;
import io.github.lunasaw.voglander.repository.entity.CascadeRecordRequestDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联录像查询请求上下文管理器。
 *
 * <p>上级发录像查询 → 登记请求（superiorSn 关联回包）→ 转查真实设备 →
 * 真实设备 RecordInfo 响应到达后，按 (localDeviceId + 时间窗) 找回请求 → 主动回包上级。
 *
 * <p>关联键说明（V5 核验结论）：server 栈录像查询命令不向调用方暴露生成的 sn，
 * 故无法用 localSn 关联，改用 (localDeviceId + startTime + endTime) 匹配 PENDING 请求。
 *
 * @author luna
 */
@Slf4j
@Component
public class CascadeRecordRequestManager {

    @Autowired
    private CascadeRecordRequestService cascadeRecordRequestService;

    /** 登记一条录像查询请求上下文，返回记录ID。superiorSn 原样回带，状态 PENDING。 */
    public Long create(String platformId, String superiorSn, CascadeChannelDTO channel, String startTime, String endTime) {
        Assert.hasText(platformId, "platformId 不能为空");
        Assert.notNull(channel, "channel 不能为空");
        CascadeRecordRequestDO doObj = new CascadeRecordRequestDO();
        doObj.setPlatformId(platformId);
        doObj.setSuperiorSn(superiorSn);
        doObj.setCascadeChannelId(channel.getCascadeChannelId());
        doObj.setLocalDeviceId(channel.getLocalDeviceId());
        doObj.setLocalChannelId(channel.getLocalChannelId());
        doObj.setStartTime(startTime);
        doObj.setEndTime(endTime);
        doObj.setStatus(CascadeConstant.RecordReqStatus.PENDING);
        cascadeRecordRequestService.save(doObj);
        return doObj.getId();
    }

    /**
     * 按 (localDeviceId + 时间窗) 找回最早的 PENDING 请求（真实响应到达时调用）。
     * 时���窗精确匹配 startTime/endTime；找不到则放宽为仅按 localDeviceId 取最早 PENDING。
     */
    public CascadeRecordRequestDTO findPending(String localDeviceId, String startTime, String endTime) {
        if (localDeviceId == null) {
            return null;
        }
        LambdaQueryWrapper<CascadeRecordRequestDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeRecordRequestDO::getLocalDeviceId, localDeviceId)
          .eq(CascadeRecordRequestDO::getStatus, CascadeConstant.RecordReqStatus.PENDING)
          .eq(startTime != null, CascadeRecordRequestDO::getStartTime, startTime)
          .eq(endTime != null, CascadeRecordRequestDO::getEndTime, endTime)
          .orderByAsc(CascadeRecordRequestDO::getCreateTime)
          .last("LIMIT 1");
        CascadeRecordRequestDO hit = cascadeRecordRequestService.getOne(qw, false);
        if (hit == null && (startTime != null || endTime != null)) {
            /* 时间窗不匹配时放宽为仅按设备取最早 PENDING（设备回包时间格式可能与请求不完全一致） */
            LambdaQueryWrapper<CascadeRecordRequestDO> relaxed = new LambdaQueryWrapper<>();
            relaxed.eq(CascadeRecordRequestDO::getLocalDeviceId, localDeviceId)
                   .eq(CascadeRecordRequestDO::getStatus, CascadeConstant.RecordReqStatus.PENDING)
                   .orderByAsc(CascadeRecordRequestDO::getCreateTime)
                   .last("LIMIT 1");
            hit = cascadeRecordRequestService.getOne(relaxed, false);
        }
        return CascadeAssembler.toDTO(hit);
    }

    public boolean markResponded(Long id) {
        return updateStatus(id, CascadeConstant.RecordReqStatus.RESPONDED);
    }

    public boolean markTimeout(Long id) {
        return updateStatus(id, CascadeConstant.RecordReqStatus.TIMEOUT);
    }

    public CascadeRecordRequestDTO getById(Long id) {
        if (id == null) {
            return null;
        }
        return CascadeAssembler.toDTO(cascadeRecordRequestService.getById(id));
    }

    /** 超时清理：将 create_time 早于 cutoff 的 PENDING 标为 TIMEOUT，返回清理条数 */
    public int cleanTimeout(LocalDateTime cutoff) {
        if (cutoff == null) {
            return 0;
        }
        LambdaQueryWrapper<CascadeRecordRequestDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeRecordRequestDO::getStatus, CascadeConstant.RecordReqStatus.PENDING)
          .lt(CascadeRecordRequestDO::getCreateTime, cutoff);
        List<CascadeRecordRequestDO> stale = cascadeRecordRequestService.list(qw);
        if (stale.isEmpty()) {
            return 0;
        }
        stale.forEach(d -> d.setStatus(CascadeConstant.RecordReqStatus.TIMEOUT));
        cascadeRecordRequestService.updateBatchById(stale);
        return stale.size();
    }

    private boolean updateStatus(Long id, int status) {
        if (id == null) {
            return false;
        }
        CascadeRecordRequestDO doObj = cascadeRecordRequestService.getById(id);
        if (doObj == null) {
            return false;
        }
        doObj.setStatus(status);
        return cascadeRecordRequestService.updateById(doObj);
    }
}
