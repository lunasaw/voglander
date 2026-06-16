package io.github.lunasaw.voglander.manager.manager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.Assert;

import io.github.lunasaw.voglander.manager.assembler.CascadeAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.service.CascadeChannelService;
import io.github.lunasaw.voglander.repository.entity.CascadeChannelDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联上报通道管理器
 */
@Slf4j
@Component
public class CascadeChannelManager {

    @Autowired
    private CascadeChannelService cascadeChannelService;

    public Long add(CascadeChannelDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        Assert.hasText(dto.getPlatformId(), "platformId 不能为空");
        Assert.hasText(dto.getLocalChannelId(), "localChannelId 不能为空");
        /* cascadeChannelId 默认同 localChannelId */
        if (dto.getCascadeChannelId() == null) {
            dto.setCascadeChannelId(dto.getLocalChannelId());
        }
        CascadeChannelDO doObj = CascadeAssembler.toDO(dto);
        cascadeChannelService.save(doObj);
        return doObj.getId();
    }

    /**
     * 批量绑定级联通道：逐条 upsert，已存在（同 platformId+localChannelId）的跳过。
     *
     * @param channels 待绑定通道列表（platformId/localChannelId 必填，cascadeChannelId 缺省=localChannelId）
     * @return 实际新增条数（跳过已存在的不计）
     */
    public int batchBind(List<CascadeChannelDTO> channels) {
        if (channels == null || channels.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (CascadeChannelDTO dto : channels) {
            if (dto == null || dto.getPlatformId() == null || dto.getLocalChannelId() == null) {
                continue;
            }
            CascadeChannelDTO existed = getByPlatformAndChannel(dto.getPlatformId(), dto.getLocalChannelId());
            if (existed != null) {
                continue;
            }
            add(dto);
            added++;
        }
        return added;
    }

    public CascadeChannelDTO getByPlatformAndChannel(String platformId, String localChannelId) {
        if (platformId == null || localChannelId == null) return null;
        LambdaQueryWrapper<CascadeChannelDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeChannelDO::getPlatformId, platformId)
          .eq(CascadeChannelDO::getLocalChannelId, localChannelId);
        return CascadeAssembler.toDTO(cascadeChannelService.getOne(qw));
    }

    /** 按上级眼中的 cascadeChannelId 查找 */
    public CascadeChannelDTO getByPlatformAndCascadeChannelId(String platformId, String cascadeChannelId) {
        if (platformId == null || cascadeChannelId == null) return null;
        LambdaQueryWrapper<CascadeChannelDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeChannelDO::getPlatformId, platformId)
          .eq(CascadeChannelDO::getCascadeChannelId, cascadeChannelId);
        return CascadeAssembler.toDTO(cascadeChannelService.getOne(qw));
    }

    public List<CascadeChannelDTO> listByPlatformId(String platformId) {
        LambdaQueryWrapper<CascadeChannelDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadeChannelDO::getPlatformId, platformId);
        return cascadeChannelService.list(qw).stream().map(CascadeAssembler::toDTO).toList();
    }

    public boolean deleteOne(CascadeChannelDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        LambdaQueryWrapper<CascadeChannelDO> qw = new LambdaQueryWrapper<>();
        qw.eq(dto.getPlatformId() != null, CascadeChannelDO::getPlatformId, dto.getPlatformId())
          .eq(dto.getLocalChannelId() != null, CascadeChannelDO::getLocalChannelId, dto.getLocalChannelId())
          .eq(dto.getId() != null, CascadeChannelDO::getId, dto.getId());
        return cascadeChannelService.remove(qw);
    }

    public CascadeChannelDTO getById(Long id) {
        if (id == null) return null;
        return CascadeAssembler.toDTO(cascadeChannelService.getById(id));
    }

    public boolean update(CascadeChannelDTO dto) {
        Assert.notNull(dto.getId(), "id 不能为空");
        CascadeChannelDO update = CascadeAssembler.toDO(dto);
        return cascadeChannelService.updateById(update);
    }

    public boolean delete(Long id) {
        Assert.notNull(id, "id 不能为空");
        return cascadeChannelService.removeById(id);
    }

    public Page<CascadeChannelDTO> getPage(CascadeChannelDTO query, int page, int size) {
        LambdaQueryWrapper<CascadeChannelDO> qw = new LambdaQueryWrapper<>();
        if (query != null) {
            qw.eq(query.getPlatformId() != null, CascadeChannelDO::getPlatformId, query.getPlatformId());
            qw.eq(query.getLocalDeviceId() != null, CascadeChannelDO::getLocalDeviceId, query.getLocalDeviceId());
            qw.eq(query.getLocalChannelId() != null, CascadeChannelDO::getLocalChannelId, query.getLocalChannelId());
            qw.eq(query.getCascadeChannelId() != null, CascadeChannelDO::getCascadeChannelId, query.getCascadeChannelId());
        }
        qw.orderByDesc(CascadeChannelDO::getCreateTime);
        Page<CascadeChannelDO> doPage = cascadeChannelService.page(new Page<>(page, size), qw);
        Page<CascadeChannelDTO> result = new Page<>(doPage.getCurrent(), doPage.getSize(), doPage.getTotal());
        result.setRecords(doPage.getRecords().stream().map(CascadeAssembler::toDTO).collect(java.util.stream.Collectors.toList()));
        return result;
    }
}
