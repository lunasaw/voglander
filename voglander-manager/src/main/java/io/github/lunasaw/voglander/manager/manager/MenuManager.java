package io.github.lunasaw.voglander.manager.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.MenuAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.service.MenuService;
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

        boolean result = menuService.save(menuDO);
        if (result) {
            log.info("创建菜单成功，菜单ID：{}", menuDO.getId());
            return menuDO.getId();
        } else {
            throw new ServiceException("创建菜单失败");
        }
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

        boolean result = menuService.updateById(menuDO);
        if (result) {
            log.info("更新菜单成功，菜单ID：{}", id);
            return true;
        } else {
            throw new ServiceException("更新菜单失败");
        }
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

        boolean result = menuService.removeById(id);
        if (result) {
            log.info("删除菜单成功，菜单ID：{}", id);
            return true;
        } else {
            throw new ServiceException("删除菜单失败");
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