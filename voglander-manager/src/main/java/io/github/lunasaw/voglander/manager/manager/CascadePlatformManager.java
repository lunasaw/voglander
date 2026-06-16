package io.github.lunasaw.voglander.manager.manager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.Assert;

import io.github.lunasaw.voglander.manager.assembler.CascadeAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.service.CascadePlatformService;
import io.github.lunasaw.voglander.repository.entity.CascadePlatformDO;
import lombok.extern.slf4j.Slf4j;

/**
 * 级联上级平台管理器
 */
@Slf4j
@Component
public class CascadePlatformManager {

    @Autowired
    private CascadePlatformService cascadePlatformService;

    public Long add(CascadePlatformDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        Assert.hasText(dto.getPlatformId(), "platformId 不能为空");
        Assert.hasText(dto.getPlatformIp(), "platformIp 不能为空");
        Assert.hasText(dto.getLocalClientId(), "localClientId 不能为空");

        CascadePlatformDO doObj = CascadeAssembler.toDO(dto);
        cascadePlatformService.save(doObj);
        return doObj.getId();
    }

    public CascadePlatformDTO getByPlatformId(String platformId) {
        if (platformId == null) return null;
        LambdaQueryWrapper<CascadePlatformDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadePlatformDO::getPlatformId, platformId);
        return CascadeAssembler.toDTO(cascadePlatformService.getOne(qw));
    }

    public CascadePlatformDTO getByLocalClientId(String localClientId) {
        if (localClientId == null) return null;
        LambdaQueryWrapper<CascadePlatformDO> qw = new LambdaQueryWrapper<>();
        qw.eq(CascadePlatformDO::getLocalClientId, localClientId);
        return CascadeAssembler.toDTO(cascadePlatformService.getOne(qw));
    }

    public boolean updateRegisterStatus(Long id, int status) {
        Assert.notNull(id, "id 不能为空");
        CascadePlatformDO update = new CascadePlatformDO();
        update.setId(id);
        update.setRegisterStatus(status);
        return cascadePlatformService.updateById(update);
    }

    public boolean deleteOne(CascadePlatformDTO dto) {
        Assert.notNull(dto, "dto 不能为空");
        LambdaQueryWrapper<CascadePlatformDO> qw = new LambdaQueryWrapper<>();
        qw.eq(dto.getPlatformId() != null, CascadePlatformDO::getPlatformId, dto.getPlatformId())
          .eq(dto.getId() != null, CascadePlatformDO::getId, dto.getId());
        return cascadePlatformService.remove(qw);
    }

    public CascadePlatformDTO getById(Long id) {
        if (id == null) return null;
        return CascadeAssembler.toDTO(cascadePlatformService.getById(id));
    }

    public boolean update(CascadePlatformDTO dto) {
        Assert.notNull(dto.getId(), "id 不能为空");
        CascadePlatformDO update = CascadeAssembler.toDO(dto);
        return cascadePlatformService.updateById(update);
    }

    public boolean delete(Long id) {
        Assert.notNull(id, "id 不能为空");
        return cascadePlatformService.removeById(id);
    }

    public boolean enablePlatform(Long id) {
        Assert.notNull(id, "id 不能为空");
        CascadePlatformDO update = new CascadePlatformDO();
        update.setId(id);
        update.setEnabled(1);
        return cascadePlatformService.updateById(update);
    }

    public boolean disablePlatform(Long id) {
        Assert.notNull(id, "id 不能为空");
        CascadePlatformDO update = new CascadePlatformDO();
        update.setId(id);
        update.setEnabled(0);
        return cascadePlatformService.updateById(update);
    }

    public Page<CascadePlatformDTO> getPage(CascadePlatformDTO query, int page, int size) {
        LambdaQueryWrapper<CascadePlatformDO> qw = new LambdaQueryWrapper<>();
        if (query != null) {
            qw.eq(query.getPlatformId() != null, CascadePlatformDO::getPlatformId, query.getPlatformId());
            qw.like(query.getPlatformIp() != null, CascadePlatformDO::getPlatformIp, query.getPlatformIp());
            qw.eq(query.getEnabled() != null, CascadePlatformDO::getEnabled, query.getEnabled());
            qw.eq(query.getRegisterStatus() != null, CascadePlatformDO::getRegisterStatus, query.getRegisterStatus());
        }
        qw.orderByDesc(CascadePlatformDO::getCreateTime);
        Page<CascadePlatformDO> doPage = cascadePlatformService.page(new Page<>(page, size), qw);
        Page<CascadePlatformDTO> result = new Page<>(doPage.getCurrent(), doPage.getSize(), doPage.getTotal());
        result.setRecords(doPage.getRecords().stream().map(CascadeAssembler::toDTO).toList());
        return result;
    }
}
