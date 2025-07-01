package io.github.lunasaw.voglander.manager.manager;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
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
import org.springframework.transaction.annotation.Transactional;

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
    private UserMapper          userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private MenuMapper          menuMapper;

    @Autowired
    private RedisCache          redisCache;

    @Autowired
    private RedisLockUtil       redisLockUtil;

    // 缓存key常量
    private static final String USER_CACHE_PREFIX            = "user:";
    private static final String USER_ROLE_CACHE_PREFIX       = "user_role:";
    private static final String USER_PERMISSION_CACHE_PREFIX = "user_permission:";
    private static final String USER_MENU_CACHE_PREFIX       = "user_menu:";
    private static final String USER_LOCK_PREFIX             = "user_lock:";

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
            .orderByAsc(MenuDO::getSortOrder);

        return menuMapper.selectList(menuWrapper);
    }

    /**
     * 删除用户角色关联
     *
     * @param userId 用户ID
     * @return 删除数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserRolesByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }

        // 调用统一删除入口方法
        return deleteUserRoleInternal(userId, "删除用户角色关联");
    }

    /**
     * 批量插入用户角色关联
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 插入数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchInsertUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null || roleIds == null || roleIds.isEmpty()) {
            return 0;
        }

        // 调用统一入口方法，批量操作需要逐个处理
        return batchInsertUserRoleInternal(userId, roleIds, "批量插入用户角色关联");
    }

    /**
     * 更新用户角色关系
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否更新成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return false;
        }

        // 调用统一入口方法
        return updateUserRoleInternal(userId, roleIds, "更新用户角色关系");
    }

    /**
     * 用户角色删除统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param userId 用户ID
     * @param operationType 操作类型描述
     * @return 删除数量
     */
    private int deleteUserRoleInternal(Long userId, String operationType) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = USER_LOCK_PREFIX + userId;

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            LambdaQueryWrapper<UserRoleDO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserRoleDO::getUserId, userId);

            int result = userRoleMapper.delete(queryWrapper);

            // 清理相关缓存
            clearUserCache(userId);

            log.info("{}成功，用户ID：{}，删除数量：{}", operationType, userId, result);
            return result;
        } catch (Exception e) {
            log.error("{}失败，用户ID：{}，错误信息：{}", operationType, userId, e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 用户角色批量插入统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @param operationType 操作类型描述
     * @return 插入数量
     */
    private int batchInsertUserRoleInternal(Long userId, List<Long> roleIds, String operationType) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notEmpty(roleIds, "角色ID列表不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = USER_LOCK_PREFIX + userId;

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
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

            // 使用批量插入，按照统一入口规范逐个处理
            int insertCount = 0;
            for (UserRoleDO userRole : userRoleList) {
                insertCount += userRoleMapper.insert(userRole);
            }

            // 清理相关缓存
            clearUserCache(userId);

            log.info("{}成功，用户ID：{}，插入数量：{}", operationType, userId, insertCount);
            return insertCount;
        } catch (Exception e) {
            log.error("{}失败，用户ID：{}，错误信息：{}", operationType, userId, e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 用户角色更新统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @param operationType 操作类型描述
     * @return 是否更新成功
     */
    private boolean updateUserRoleInternal(Long userId, List<Long> roleIds, String operationType) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = USER_LOCK_PREFIX + userId;

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            // 先删除现有的用户角色关系
            deleteUserRoleInternal(userId, "删除用户角色关系");

            // 如果角色列表不为空，则插入新的用户角色关系
            if (roleIds != null && !roleIds.isEmpty()) {
                batchInsertUserRoleInternal(userId, roleIds, "插入用户角色关系");
            }

            // 清理相关缓存
            clearUserCache(userId);

            log.info("{}成功，用户ID：{}，角色数量：{}", operationType, userId,
                roleIds != null ? roleIds.size() : 0);

            return true;
        } catch (Exception e) {
            log.error("{}失败，用户ID：{}，错误信息：{}", operationType, userId, e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 清理用户相关缓存
     *
     * @param userId 用户ID
     */
    private void clearUserCache(Long userId) {
        try {
            if (userId != null) {
                // 清理用户基本信息缓存
                redisCache.deleteKey(USER_CACHE_PREFIX + userId);

                // 清理用户角色缓存
                redisCache.deleteKey(USER_ROLE_CACHE_PREFIX + userId);

                // 清理用户权限缓存
                redisCache.deleteKey(USER_PERMISSION_CACHE_PREFIX + userId);

                // 清理用户菜单缓存
                redisCache.deleteKey(USER_MENU_CACHE_PREFIX + userId);
            }

            log.debug("清理用户缓存成功，用户ID：{}", userId);
        } catch (Exception e) {
            log.error("清理用户缓存失败，用户ID：{}，错误信息：{}", userId, e.getMessage());
        }
    }
}