package io.github.lunasaw.voglander.manager.manager;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.MenuAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import io.github.lunasaw.voglander.repository.cache.redis.RedisLockUtil;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单管理器
 * 处理菜单相关的复杂业务逻辑
 *
 * @author luna
 */
@Slf4j
@Component
public class MenuManager {

    @Autowired
    private UserManager userManager;

    @Autowired
    private MenuService menuService;

    @Autowired
    private RedisCache          redisCache;

    @Autowired
    private RedisLockUtil       redisLockUtil;

    // 缓存key常量
    private static final String MENU_CACHE_PREFIX      = "menu:";
    private static final String USER_MENU_CACHE_PREFIX = "user_menu:";
    private static final String MENU_LIST_CACHE_KEY    = "menu_list:all";
    private static final String MENU_LOCK_PREFIX       = "menu_lock:";

    /**
     * 根据用户ID获取用户菜单
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    public List<MenuDTO> getUserMenus(Long userId) {
        List<MenuDO> menuDOList = userManager.getUserMenus(userId);
        return MenuAssembler.toDTOList(menuDOList);
    }

    /**
     * 构建菜单树
     *
     * @param menuList 菜单列表
     * @return 菜单树
     */
    public List<MenuDTO> buildMenuTree(List<MenuDTO> menuList) {
        return MenuAssembler.buildMenuTree(menuList);
    }

    /**
     * 获取所有菜单列表
     *
     * @return 菜单列表
     */
    public List<MenuDTO> getAllMenus() {
        LambdaQueryWrapper<MenuDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MenuDO::getStatus, 1)
            .orderByAsc(MenuDO::getSortOrder)
            .orderByAsc(MenuDO::getId);

        List<MenuDO> menuDOList = menuService.list(queryWrapper);
        return MenuAssembler.toDTOList(menuDOList);
    }

    /**
     * 根据ID获取菜单
     *
     * @param id 菜单ID
     * @return 菜单信息
     */
    public MenuDTO getMenuById(Long id) {
        Assert.notNull(id, "菜单ID不能为空");

        MenuDO menuDO = menuService.getById(id);
        return MenuAssembler.toDTO(menuDO);
    }

    /**
     * 创建菜单
     *
     * @param menuDTO 菜单信息
     * @return 创建的菜单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(MenuDTO menuDTO) {
        Assert.notNull(menuDTO, "菜单信息不能为空");
        Assert.hasText(menuDTO.getMenuName(), "菜单名称不能为空");
        Assert.hasText(menuDTO.getMenuCode(), "菜单编码不能为空");

        // 检查菜单名称是否重复
        if (isMenuNameExists(menuDTO.getMenuName(), null)) {
            throw new ServiceException("菜单名称已存在");
        }

        // 检查菜单路径是否重复
        if (StringUtils.isNotBlank(menuDTO.getPath()) && isMenuPathExists(menuDTO.getPath(), null)) {
            throw new ServiceException("菜单路径已存在");
        }

        MenuDO menuDO = MenuAssembler.toDO(menuDTO);
        menuDO.setCreateTime(LocalDateTime.now());
        menuDO.setUpdateTime(LocalDateTime.now());

        // 调用统一入口方法
        return menuInternal(menuDO, "创建菜单");
    }

    /**
     * 更新菜单
     *
     * @param id 菜单ID
     * @param menuDTO 菜单信息
     * @return 是否更新成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(Long id, MenuDTO menuDTO) {
        Assert.notNull(id, "菜单ID不能为空");
        Assert.notNull(menuDTO, "菜单信息不能为空");

        MenuDO existMenu = menuService.getById(id);
        if (existMenu == null) {
            throw new ServiceException("菜单不存在");
        }

        // 检查菜单名称是否重复
        if (StringUtils.isNotBlank(menuDTO.getMenuName()) &&
            isMenuNameExists(menuDTO.getMenuName(), id)) {
            throw new ServiceException("菜单名称已存在");
        }

        // 检查菜单路径是否重复
        if (StringUtils.isNotBlank(menuDTO.getPath()) &&
            isMenuPathExists(menuDTO.getPath(), id)) {
            throw new ServiceException("菜单路径已存在");
        }

        MenuDO menuDO = MenuAssembler.toDO(menuDTO);
        menuDO.setId(id);
        menuDO.setUpdateTime(LocalDateTime.now());

        // 调用统一入口方法
        Long result = menuInternal(menuDO, "更新菜单");
        return result != null;
    }

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long id) {
        Assert.notNull(id, "菜单ID不能为空");

        MenuDO menuDO = menuService.getById(id);
        if (menuDO == null) {
            throw new ServiceException("菜单不存在");
        }

        // 检查是否有子菜单
        LambdaQueryWrapper<MenuDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MenuDO::getParentId, id);
        long childCount = menuService.count(queryWrapper);
        if (childCount > 0) {
            throw new ServiceException("该菜单下还有子菜单，无法删除");
        }

        // 调用统一删除入口方法
        return deleteMenuInternal(id, "删除菜单");
    }

    /**
     * 菜单数据操作统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param menuDO 菜单数据对象
     * @param operationType 操作类型描述
     * @return 菜单ID
     */
    private Long menuInternal(MenuDO menuDO, String operationType) {
        Assert.notNull(menuDO, "菜单数据不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = MENU_LOCK_PREFIX + (menuDO.getId() != null ? menuDO.getId() : "new");

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean isUpdate = menuDO.getId() != null;
            boolean result;

            if (isUpdate) {
                result = menuService.updateById(menuDO);
            } else {
                result = menuService.save(menuDO);
            }

            if (result) {
                // 清理相关缓存
                clearMenuCache(menuDO.getId());

                log.info("{}成功，菜单ID：{}，菜单名称：{}", operationType, menuDO.getId(), menuDO.getMenuName());
                return menuDO.getId();
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，菜单ID：{}，错误信息：{}", operationType, menuDO.getId(), e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 菜单删除统一入口 - 负责统一的缓存清理、日志记录和数据校验
     *
     * @param menuId 菜单ID
     * @param operationType 操作类型描述
     * @return 是否删除成功
     */
    private boolean deleteMenuInternal(Long menuId, String operationType) {
        Assert.notNull(menuId, "菜单ID不能为空");
        Assert.hasText(operationType, "操作类型不能为空");

        String lockKey = MENU_LOCK_PREFIX + menuId;

        try {
            // 获取分布式锁
            if (!redisLockUtil.tryLock(lockKey, 5)) {
                throw new ServiceException("系统繁忙，请稍后重试");
            }

            boolean result = menuService.removeById(menuId);

            if (result) {
                // 清理相关缓存
                clearMenuCache(menuId);

                log.info("{}成功，菜单ID：{}", operationType, menuId);
                return true;
            } else {
                throw new ServiceException(operationType + "失败");
            }
        } catch (Exception e) {
            log.error("{}失败，菜单ID：{}，错误信息：{}", operationType, menuId, e.getMessage());
            throw e;
        } finally {
            // 释放分布式锁
            redisLockUtil.unLock(lockKey);
        }
    }

    /**
     * 清理菜单相关缓存
     *
     * @param menuId 菜单ID（可为null）
     */
    private void clearMenuCache(Long menuId) {
        try {
            // 清理单个菜单缓存
            if (menuId != null) {
                redisCache.deleteKey(MENU_CACHE_PREFIX + menuId);
            }

            // 清理菜单列表缓存
            redisCache.deleteKey(MENU_LIST_CACHE_KEY);

            // 清理用户菜单缓存（通过pattern匹配）
            redisCache.deleteKey(redisCache.keys(USER_MENU_CACHE_PREFIX + "*"));

            log.debug("清理菜单缓存成功，菜单ID：{}", menuId);
        } catch (Exception e) {
            log.error("清理菜单缓存失败，菜单ID：{}，错误信息：{}", menuId, e.getMessage());
        }
    }

    /**
     * 检查菜单名称是否存在
     *
     * @param name 菜单名称
     * @param excludeId 排除的菜单ID
     * @return 是否存在
     */
    public boolean isMenuNameExists(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            return false;
        }

        LambdaQueryWrapper<MenuDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MenuDO::getMenuName, name);
        if (excludeId != null) {
            queryWrapper.ne(MenuDO::getId, excludeId);
        }

        return menuService.count(queryWrapper) > 0;
    }

    /**
     * 检查菜单路径是否存在
     *
     * @param path 菜单路径
     * @param excludeId 排除的菜单ID
     * @return 是否存在
     */
    public boolean isMenuPathExists(String path, Long excludeId) {
        if (StringUtils.isBlank(path)) {
            return false;
        }

        LambdaQueryWrapper<MenuDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MenuDO::getPath, path);
        if (excludeId != null) {
            queryWrapper.ne(MenuDO::getId, excludeId);
        }

        return menuService.count(queryWrapper) > 0;
    }
}