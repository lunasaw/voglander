package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.manager.assembler.CascadeAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeSubscribeDTO;
import io.github.lunasaw.voglander.manager.service.CascadeSubscribeService;
import io.github.lunasaw.voglander.repository.entity.CascadeSubscribeDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联上级订阅管理器。
 *
 * <p>记录「哪个上级平台订阅了本平台哪类信息」，是主动推送的目标清单来源。
 * UNIQUE(platform_id, sub_type) 保证一个上级对一类信息只有一条 ACTIVE 记录。
 *
 * @author luna
 */
@Slf4j
@Component
public class CascadeSubscribeManager {

    @Autowired
    private CascadeSubscribeService cascadeSubscribeService;

    /**
     * 登记/续订一条订阅（UNIQUE(platform,type) 前查后插，避免唯一约束冲突）。
     * 回填 expireTime=now+expires，status=ACTIVE。
     */
    public Long upsertActive(String platformId, CascadeConstant.SubType type, String sn, Integer expires, Integer interval) {
        Assert.hasText(platformId, "platformId 不能为空");
        Assert.notNull(type, "subType 不能为空");
        int effectiveExpires = (expires != null && expires > 0) ? expires : CascadeConstant.DEFAULT_SUBSCRIBE_EXPIRES;

        CascadeSubscribeDO existing = getDOByPlatformAndType(platformId, type);
        CascadeSubscribeDO doObj = existing != null ? existing : new CascadeSubscribeDO();
        doObj.setPlatformId(platformId);
        doObj.setSubType(type.name());
        doObj.setSn(sn);
        doObj.setExpires(effectiveExpires);
        doObj.setIntervalSec(interval);
        doObj.setExpireTime(LocalDateTime.now().plusSeconds(effectiveExpires));
        doObj.setStatus(CascadeConstant.SubStatus.ACTIVE);

        cascadeSubscribeService.saveOrUpdate(doObj);
        return doObj.getId();
    }

    /** 退订/过期：status=EXPIRED */
    public boolean expire(String platformId, CascadeConstant.SubType type) {
        Assert.hasText(platformId, "platformId 不能为空");
        Assert.notNull(type, "subType 不能为空");
        CascadeSubscribeDO existing = getDOByPlatformAndType(platformId, type);
        if (existing == null) {
            return false;
        }
        existing.setStatus(CascadeConstant.SubStatus.EXPIRED);
        return cascadeSubscribeService.updateById(existing);
    }

    /** 主动推送时取目标上级清单：某类信息的全部 ACTIVE 订阅 */
    public List<CascadeSubscribeDTO> listActiveByType(CascadeConstant.SubType type) {
        Assert.notNull(type, "subType 不能为空");
        LambdaQueryWrapper<CascadeSubscribeDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeSubscribeDO::getSubType, type.name())
          .eq(CascadeSubscribeDO::getStatus, CascadeConstant.SubStatus.ACTIVE);
        return cascadeSubscribeService.list(qw).stream().map(CascadeAssembler::toDTO).toList();
    }

    /** 平台维度的全部 ACTIVE 订阅 */
    public List<CascadeSubscribeDTO> listActiveByPlatform(String platformId) {
        if (platformId == null) {
            return List.of();
        }
        LambdaQueryWrapper<CascadeSubscribeDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeSubscribeDO::getPlatformId, platformId)
          .eq(CascadeSubscribeDO::getStatus, CascadeConstant.SubStatus.ACTIVE);
        return cascadeSubscribeService.list(qw).stream().map(CascadeAssembler::toDTO).toList();
    }

    public CascadeSubscribeDTO getByPlatformAndType(String platformId, CascadeConstant.SubType type) {
        return CascadeAssembler.toDTO(getDOByPlatformAndType(platformId, type));
    }

    /** 过期清理：将 expire_time 早于 now 的 ACTIVE 记录标为 EXPIRED，返回清理条数 */
    public int cleanExpired(LocalDateTime now) {
        LocalDateTime cutoff = now != null ? now : LocalDateTime.now();
        LambdaQueryWrapper<CascadeSubscribeDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeSubscribeDO::getStatus, CascadeConstant.SubStatus.ACTIVE)
          .isNotNull(CascadeSubscribeDO::getExpireTime)
          .lt(CascadeSubscribeDO::getExpireTime, cutoff);
        List<CascadeSubscribeDO> expired = cascadeSubscribeService.list(qw);
        if (expired.isEmpty()) {
            return 0;
        }
        expired.forEach(d -> d.setStatus(CascadeConstant.SubStatus.EXPIRED));
        cascadeSubscribeService.updateBatchById(expired);
        return expired.size();
    }

    private CascadeSubscribeDO getDOByPlatformAndType(String platformId, CascadeConstant.SubType type) {
        if (platformId == null || type == null) {
            return null;
        }
        LambdaQueryWrapper<CascadeSubscribeDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeSubscribeDO::getPlatformId, platformId)
          .eq(CascadeSubscribeDO::getSubType, type.name());
        return cascadeSubscribeService.getOne(qw, false);
    }
}
