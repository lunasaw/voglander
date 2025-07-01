package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.RoleAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.RoleDTO;
import io.github.lunasaw.voglander.manager.service.RoleService;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.entity.RoleDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import io.github.lunasaw.voglander.repository.mapper.RoleMapper;
import lombok.extern.slf4j.Slf4j;
import com.luna.common.check.Assert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理器
 * 处理角色相关的复杂业务逻辑
 *
 * <p>
 * 架构设计：
 * </p>
 * <ul>
 * <li>统一修改入口：{@link #roleInternal(RoleDO, String)} - 所有修改操作的核心方法</li>
 * <li>统一删除入口：{@link #deleteRoleInternal(Long, String)} - 所有删除操作的核心方法</li>
 * <li>统一缓存管理：{@link #clearRoleCache(Long)} - 统一的缓存清理逻辑</li>
 * </ul>
 *
 * <p>
 * 优势：
 * </p>
 * <ul>
 * <li>缓存一致性：所有修改和删除操作都通过统一入口，确保缓存清理的一致性</li>
 * <li>日志规范：统一的日志记录格式和内容</li>
 * <li>异常处理：统一的数据校验和异常处理逻辑</li>
 * <li>维护便捷：业务逻辑变更只需修改核心方法</li>
 * </ul>
 *
 * @author luna
 */
@Slf4j
@Component
public class RoleManager {

    /**
     * 角色缓存Key前缀
     */
    private static final String ROLE_CACHE_PREFIX      = "role:";

    /**
     * 角色列表缓存Key
     */
    private static final String ROLE_LIST_CACHE_KEY    = "role:list";

    /**
     * 用户角色缓存Key前缀
     */
    private static final String USER_ROLE_CACHE_PREFIX = "user:role:";

    /**
     * 角色分布式锁前缀
     */
    private static final String ROLE_LOCK_PREFIX       = "role:lock:";

    @Autowired
    private MenuMapper          menuMapper;

    @Autowired
    private RoleMapper          roleMapper;

    @Autowired
    private RoleService         roleService;

    @Autowired
    private RedisCache          redisCache;

    @Autowired
    private RedisLockUtil       redisLockUtil;

    /**
     * 分页查询角色列表（包含权限信息）
     *
     * @param dto 查询条件
     * @return 角色列表分页数据
     */
    public IPage<RoleDTO> getRoleList(RoleDTO dto) {
        Page<RoleDO> page = new Page<>(
            dto.getPageNum() != null ? dto.getPageNum() : 1,
            dto.getPageSize() != null ? dto.getPageSize() : 10);

        // 构建查询条件
        LambdaQueryWrapper<RoleDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(dto.getRoleName()), RoleDO::getRoleName, dto.getRoleName())
            .eq(dto.getStatus() != null, RoleDO::getStatus, dto.getStatus())
            .orderByDesc(RoleDO::getCreateTime);

        // 使用RoleService进行分页查询，符合规范
        IPage<RoleDO> rolePage = roleService.page(page, queryWrapper);

        // 转换结果并填充权限信息
        IPage<RoleDTO> result = new Page<>();
        result.setCurrent(rolePage.getCurrent());
        result.setSize(rolePage.getSize());
        result.setTotal(rolePage.getTotal());
        result.setPages(rolePage.getPages());

        List<RoleDTO> roleDTOList = RoleAssembler.toDTOList(rolePage.getRecords());

        // 为每个角色查询并设置权限信息
        if (roleDTOList != null) {
            roleDTOList.forEach(roleDTO -> {
                List<MenuDO> menuList = menuMapper.selectMenusByRoleId(roleDTO.getId());
                roleDTO.setPermissions(RoleAssembler.menuListToPermissionIds(menuList));
            });
        }

        result.setRecords(roleDTOList);
        return result;
    }

    /**
     * 根据ID获取角色信息（包含权限信息）
     *
     * @param id 角色ID
     * @return 角色信息
     */
    public RoleDTO getRoleById(Long id) {
        if (id == null) {
            return null;
        }

        // 使用RoleService进行查询，符合规范
        RoleDO roleDO = roleService.getById(id);
        if (roleDO == null) {
            return null;
        }

        RoleDTO dto = RoleAssembler.toDTO(roleDO);

        // 查询角色权限
        List<MenuDO> menuList = menuMapper.selectMenusByRoleId(id);
        dto.setPermissions(RoleAssembler.menuListToPermissionIds(menuList));

        return dto;
    }

    /**
     * 创建角色（包含权限关联）
     *
     * @param dto 角色信息
     * @return 创建结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean createRole(RoleDTO dto) {
        Assert.notNull(dto, "角色信息不能为空");
        Assert.hasText(dto.getRoleName(), "角色名称不能为空");

        RoleDO roleDO = RoleAssembler.createRoleDO(dto);
        roleDO.setCreateTime(LocalDateTime.now());

        // 调用统一入口方法
        Long roleId = roleInternal(roleDO, "创建角色");

        if (roleId != null) {
            // 创建角色权限关联
            if (dto.getPermissions() != null && !dto.getPermissions().isEmpty()) {
                updateRolePermissions(roleId, dto.getPermissions());
            }
            return true;
        }
        return false;
    }

    /**
     * 更新角色（包含权限关联）
     *
     * @param id 角色ID
     * @param dto 角色信息
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(Long id, RoleDTO dto) {
        Assert.notNull(id, "角色ID不能为空");
        Assert.notNull(dto, "角色信息不能为空");

        RoleDO existingRole = roleService.getById(id);
        if (existingRole == null) {
            throw new ServiceException("角色不存在");
        }

        // 更新角色基本信息
        RoleAssembler.updateRoleDO(existingRole, dto);
        existingRole.setUpdateTime(LocalDateTime.now());

        // 调用统一入口方法
        Long roleId = roleInternal(existingRole, "更新角色");

        if (roleId != null) {
            // 更新角色权限关联
            updateRolePermissions(id, dto.getPermissions());
            return true;
        }
        return false;
    }

    /**
     * 删除角色（包含权限关联）
     *
     * @param id 角色ID
     * @return 删除结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long id) {
        Assert.notNull(id, "角色ID不能为空");

        RoleDO role = roleService.getById(id);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }

        // 检查是否有用户使用该角色
        // 这里可以根据业务需求添加检查逻辑

        // 调用统一删除入口方法
        return deleteRoleInternal(id, "删除角色");
    }

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<RoleDTO> getRolesByUserId(Long userId) {
        Assert.notNull(userId, "用户ID不能为空");

        List<RoleDO> roleList = roleMapper.selectRolesByUserId(userId);
        return RoleAssembler.toDTOList(roleList);
    }

    /**
     * 角色数据操作统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param roleDO 角色数据对象
     * @param operationType 操作类型描述
     * @return 角色ID
     */
    private Long roleInternal(RoleDO roleDO, String operationType) {
        Assert.notNull(roleDO, "角色数据不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = ROLE_LOCK_PREFIX + (roleDO.getId() != null ? roleDO.getId() : "new");

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean isUpdate = roleDO.getId() != null;
            boolean result;

            if (isUpdate) {
                result = roleService.updateById(roleDO);
            } else {
                result = roleService.save(roleDO);
            }

            if (result) {
                // 清理相关缓存
                clearRoleCache(roleDO.getId());

                log.info("{}成功，角色ID：{}，角色名称：{}", operationType, roleDO.getId(), roleDO.getRoleName());
                return roleDO.getId();
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，角色ID：{}，错误信息：{}", operationType, roleDO.getId(), e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 角色删除统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param roleId 角色ID
     * @param operationType 操作类型描述
     * @return 是否删除成功
     */
    private boolean deleteRoleInternal(Long roleId, String operationType) {
        Assert.notNull(roleId, "角色ID不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = ROLE_LOCK_PREFIX + roleId;

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            // 先删除角色权限关联
            roleMapper.deleteRoleMenuByRoleId(roleId);

            boolean result = roleService.removeById(roleId);

            if (result) {
                // 清理相关缓存
                clearRoleCache(roleId);

                log.info("{}成功，角色ID：{}", operationType, roleId);
                return true;
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，角色ID：{}，错误信息：{}", operationType, roleId, e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 清理角色相关缓存
     *
     * @param roleId 角色ID（可为null）
     */
    private void clearRoleCache(Long roleId) {
        try {
            // 清理单个角色缓存
            if (roleId != null) {
                redisCache.deleteKey(ROLE_CACHE_PREFIX + roleId);
            }

            // 清理角色列表缓存
            redisCache.deleteKey(ROLE_LIST_CACHE_KEY);

            // 清理用户角色缓存（通过pattern匹配）
            redisCache.deleteKey(redisCache.keys(USER_ROLE_CACHE_PREFIX + "*"));

            log.debug("清理角色缓存成功，角色ID：{}", roleId);
        } catch (Exception e) {
            log.error("清理角色缓存失败，角色ID：{}，错误信息：{}", roleId, e.getMessage());
        }
    }

    /**
     * 更新角色权限关联
     *
     * @param roleId 角色ID
     * @param permissions 权限列表（菜单ID列表）
     */
    private void updateRolePermissions(Long roleId, List<Long> permissions) {
        // 先删除该角色的所有权限关联
        roleMapper.deleteRoleMenuByRoleId(roleId);

        // 如果有新的权限，批量插入
        if (permissions != null && !permissions.isEmpty()) {
            roleMapper.batchInsertRoleMenu(roleId, permissions);
            log.info("更新角色权限成功，角色ID：{}，权限菜单ID：{}", roleId, permissions);
        } else {
            log.info("清空角色权限，角色ID：{}", roleId);
        }

        // 权限更新后需要清理相关缓存
        clearRoleCache(roleId);
    }
}