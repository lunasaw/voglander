package io.github.lunasaw.voglander.manager.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.entity.RoleDO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色数据转换器
 *
 * @author luna
 */
public class RoleAssembler {

    /**
     * DO转DTO
     */
    public static RoleDTO toDTO(RoleDO roleDO) {
        if (roleDO == null) {
            return null;
        }

        RoleDTO dto = new RoleDTO();
        dto.setId(roleDO.getId());
        dto.setCreateTime(roleDO.getCreateTime());
        dto.setUpdateTime(roleDO.getUpdateTime());
        dto.setRoleName(roleDO.getRoleName());
        dto.setDescription(roleDO.getDescription());
        dto.setStatus(roleDO.getStatus());
        dto.setExtend(roleDO.getExtend());
        return dto;
    }

    /**
     * DTO转DO
     */
    public static RoleDO toDO(RoleDTO dto) {
        if (dto == null) {
            return null;
        }

        RoleDO roleDO = new RoleDO();
        roleDO.setId(dto.getId());
        roleDO.setCreateTime(dto.getCreateTime());
        roleDO.setUpdateTime(dto.getUpdateTime());
        roleDO.setRoleName(dto.getRoleName());
        roleDO.setDescription(dto.getDescription());
        roleDO.setStatus(dto.getStatus());
        roleDO.setExtend(dto.getExtend());
        return roleDO;
    }

    /**
     * DO列表转DTO列表
     */
    public static List<RoleDTO> toDTOList(List<RoleDO> roleList) {
        if (roleList == null) {
            return null;
        }
        return roleList.stream().map(RoleAssembler::toDTO).collect(Collectors.toList());
    }

    /**
     * 创建角色DO（设置默认值）
     */
    public static RoleDO createRoleDO(RoleDTO dto) {
        if (dto == null) {
            return null;
        }

        RoleDO roleDO = new RoleDO();
        roleDO.setRoleName(dto.getRoleName());
        roleDO.setDescription(dto.getDescription());
        roleDO.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        roleDO.setExtend(dto.getExtend());
        roleDO.setCreateTime(LocalDateTime.now());
        roleDO.setUpdateTime(LocalDateTime.now());
        return roleDO;
    }

    /**
     * 更新角色DO
     */
    public static void updateRoleDO(RoleDO existingRole, RoleDTO dto) {
        if (existingRole == null || dto == null) {
            return;
        }

        existingRole.setRoleName(dto.getRoleName());
        existingRole.setDescription(dto.getDescription());
        existingRole.setStatus(dto.getStatus());
        existingRole.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 菜单列表转权限列表（权限标识符）
     */
    public static List<String> menuListToPermissions(List<MenuDO> menuList) {
        if (menuList == null) {
            return null;
        }
        return menuList.stream()
            .map(MenuDO::getPermission)
            .filter(permission -> permission != null && !permission.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * 菜单列表转权限ID列表
     */
    public static List<Long> menuListToPermissionIds(List<MenuDO> menuList) {
        if (menuList == null) {
            return null;
        }
        return menuList.stream()
            .map(MenuDO::getId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
    }

    /**
     * 权限标识符列表转菜单ID列表
     *
     * @param permissions 权限标识符列表
     * @param menuList 所有菜单列表
     * @return 菜单ID列表
     */
    public static List<Long> permissionsToMenuIds(List<String> permissions, List<MenuDO> menuList) {
        if (permissions == null || menuList == null) {
            return null;
        }
        return menuList.stream()
            .filter(menu -> permissions.contains(menu.getPermission()))
            .map(MenuDO::getId)
            .collect(Collectors.toList());
    }
}
