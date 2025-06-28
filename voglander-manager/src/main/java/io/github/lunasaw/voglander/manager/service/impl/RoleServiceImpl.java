package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.manager.manager.RoleManager;
import io.github.lunasaw.voglander.manager.service.RoleService;
import io.github.lunasaw.voglander.repository.entity.RoleDO;
import io.github.lunasaw.voglander.repository.mapper.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RoleDO> implements RoleService {

    @Autowired
    private RoleManager roleManager;

    @Override
    public IPage<RoleDTO> getRoleList(RoleDTO dto) {
        return roleManager.getRoleList(dto);
    }

    @Override
    public RoleDTO getRoleById(Long id) {
        return roleManager.getRoleById(id);
    }

    @Override
    public boolean createRole(RoleDTO dto) {
        return roleManager.createRole(dto);
    }

    @Override
    public boolean updateRole(Long id, RoleDTO dto) {
        if (getById(id) == null) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "角色不存在");
        }
        return roleManager.updateRole(id, dto);
    }

    @Override
    public boolean deleteRole(Long id) {
        if (getById(id) == null) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "角色不存在");
        }
        return roleManager.deleteRole(id);
    }

    @Override
    public List<RoleDTO> getRolesByUserId(Long userId) {
        return roleManager.getRolesByUserId(userId);
    }
}