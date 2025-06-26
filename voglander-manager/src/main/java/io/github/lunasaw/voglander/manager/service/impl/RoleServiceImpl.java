package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.assembler.RoleAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.manager.service.RoleService;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.entity.RoleDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import io.github.lunasaw.voglander.repository.mapper.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public IPage<RoleDTO> getRoleList(RoleDTO dto) {
        Page<RoleDO> page = new Page<>(
            dto.getPageNum() != null ? dto.getPageNum() : 1,
            dto.getPageSize() != null ? dto.getPageSize() : 10);

        // 构建查询条件
        LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(dto.getRoleName()), RoleDO::getRoleName, dto.getRoleName())
            .like(StringUtils.isNotBlank(dto.getRoleCode()), RoleDO::getRoleCode, dto.getRoleCode())
            .eq(dto.getStatus() != null, RoleDO::getStatus, dto.getStatus())
            .orderByDesc(RoleDO::getCreateTime);

        IPage<RoleDO> rolePage = roleMapper.selectPage(page, queryWrapper);

        // 转换结果
        IPage<RoleDTO> result = new Page<>();
        result.setCurrent(rolePage.getCurrent());
        result.setSize(rolePage.getSize());
        result.setTotal(rolePage.getTotal());
        result.setPages(rolePage.getPages());
        result.setRecords(RoleAssembler.toDTOList(rolePage.getRecords()));

        return result;
    }

    @Override
    public RoleDTO getRoleById(Long id) {
        if (id == null) {
            return null;
        }

        RoleDO roleDO = roleMapper.selectById(id);
        if (roleDO == null) {
            return null;
        }

        RoleDTO dto = RoleAssembler.toDTO(roleDO);

        // 查询角色权限
        List<MenuDO> menuList = menuMapper.selectMenusByRoleId(id);
        dto.setPermissions(RoleAssembler.menuListToPermissions(menuList));

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRole(RoleDTO dto) {
        // 校验角色编码唯一性
        if (!isRoleCodeUnique(dto.getRoleCode(), null)) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "角色编码已存在");
        }

        RoleDO roleDO = RoleAssembler.createRoleDO(dto);
        int result = roleMapper.insert(roleDO);

        if (result > 0) {
            log.info("创建角色成功，角色ID：{}", roleDO.getId());
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(Long id, RoleDTO dto) {
        RoleDO existingRole = roleMapper.selectById(id);
        if (existingRole == null) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "角色不存在");
        }

        // 校验角色编码唯一性（排除当前角色）
        if (!isRoleCodeUnique(dto.getRoleCode(), id)) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "角色编码已存在");
        }

        RoleAssembler.updateRoleDO(existingRole, dto);
        int result = roleMapper.updateById(existingRole);

        if (result > 0) {
            log.info("更新角色成功，角色ID：{}", id);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long id) {
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "角色不存在");
        }

        // 检查是否有用户使用该角色
        // 这里可以根据业务需求添加检查逻辑

        int result = roleMapper.deleteById(id);
        if (result > 0) {
            log.info("删除角色成功，角色ID：{}", id);
            return true;
        }
        return false;
    }

    @Override
    public List<RoleDTO> getRolesByUserId(Long userId) {
        List<RoleDO> roleList = roleMapper.selectRolesByUserId(userId);
        return RoleAssembler.toDTOList(roleList);
    }

    @Override
    public boolean isRoleCodeUnique(String roleCode, Long excludeId) {
        if (StringUtils.isBlank(roleCode)) {
            return false;
        }

        LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RoleDO::getRoleCode, roleCode);
        if (excludeId != null) {
            queryWrapper.ne(RoleDO::getId, excludeId);
        }

        return roleMapper.selectCount(queryWrapper) == 0;
    }
}