package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.entity.RoleMenuDO;
import io.github.lunasaw.voglander.repository.entity.UserDO;
import io.github.lunasaw.voglander.repository.entity.UserRoleDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import io.github.lunasaw.voglander.repository.mapper.RoleMenuMapper;
import io.github.lunasaw.voglander.repository.mapper.UserMapper;
import io.github.lunasaw.voglander.repository.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户Manager
 * 封装用户相关的复杂业务逻辑，特别是多表关联查询
 *
 * @author luna
 */
@Slf4j
@Component
public class UserManager {

    @Autowired
    private UserMapper     userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private MenuMapper     menuMapper;

    /**
     * 根据用户名查询用户（启用状态）
     *
     * @param username 用户名
     * @return 用户信息
     */
    public UserDO getUserByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }

        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserDO::getUsername, username)
            .eq(UserDO::getStatus, 1);

        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 根据用户ID查询角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    public List<Long> getUserRoleIds(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<UserRoleDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRoleDO::getUserId, userId);

        List<UserRoleDO> userRoleList = userRoleMapper.selectList(queryWrapper);
        return userRoleList.stream()
            .map(UserRoleDO::getRoleId)
            .collect(Collectors.toList());
    }

    /**
     * 根据用户ID查询用户权限
     *
     * @param userId 用户ID
     * @return 权限码列表
     */
    public List<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. 查询用户的角色ID列表
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询角色对应的菜单ID列表
        LambdaQueryWrapper<RoleMenuDO> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(RoleMenuDO::getRoleId, roleIds);

        List<RoleMenuDO> roleMenuList = roleMenuMapper.selectList(roleMenuWrapper);
        if (roleMenuList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> menuIds = roleMenuList.stream()
            .map(RoleMenuDO::getMenuId)
            .collect(Collectors.toSet());

        // 3. 查询菜单权限标识
        LambdaQueryWrapper<MenuDO> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(MenuDO::getId, menuIds)
            .eq(MenuDO::getStatus, 1)
            .isNotNull(MenuDO::getPermission)
            .ne(MenuDO::getPermission, "");

        List<MenuDO> menuList = menuMapper.selectList(menuWrapper);
        return menuList.stream()
            .map(MenuDO::getPermission)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 根据用户ID查询用户菜单
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    public List<MenuDO> getUserMenus(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. 查询用户的角色ID列表
        List<Long> roleIds = getUserRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询角色对应的菜单ID列表
        LambdaQueryWrapper<RoleMenuDO> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(RoleMenuDO::getRoleId, roleIds);

        List<RoleMenuDO> roleMenuList = roleMenuMapper.selectList(roleMenuWrapper);
        if (roleMenuList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> menuIds = roleMenuList.stream()
            .map(RoleMenuDO::getMenuId)
            .collect(Collectors.toSet());

        // 3. 查询菜单信息
        LambdaQueryWrapper<MenuDO> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(MenuDO::getId, menuIds)
            .eq(MenuDO::getStatus, 1)
            .eq(MenuDO::getVisible, 1)
            .orderByAsc(MenuDO::getSortOrder);

        return menuMapper.selectList(menuWrapper);
    }

    /**
     * 删除用户角色关联
     *
     * @param userId 用户ID
     * @return 删除数量
     */
    public int deleteUserRolesByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }

        LambdaQueryWrapper<UserRoleDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRoleDO::getUserId, userId);

        return userRoleMapper.delete(queryWrapper);
    }

    /**
     * 批量插入用户角色关联
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 插入数量
     */
    public int batchInsertUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null || roleIds == null || roleIds.isEmpty()) {
            return 0;
        }

        List<UserRoleDO> userRoleList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Long roleId : roleIds) {
            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreateTime(now);
            userRoleList.add(userRole);
        }

        // 使用批量插入
        int insertCount = 0;
        for (UserRoleDO userRole : userRoleList) {
            insertCount += userRoleMapper.insert(userRole);
        }

        return insertCount;
    }

    /**
     * 更新用户角色关系
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否更新成功
     */
    public boolean updateUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return false;
        }

        // 先删除现有的用户角色关系
        deleteUserRolesByUserId(userId);

        // 如果角色列表不为空，则插入新的用户角色关系
        if (roleIds != null && !roleIds.isEmpty()) {
            batchInsertUserRoles(userId, roleIds);
        }

        log.info("更新用户角色关系成功，用户ID：{}，角色数量：{}", userId,
            roleIds != null ? roleIds.size() : 0);

        return true;
    }
}