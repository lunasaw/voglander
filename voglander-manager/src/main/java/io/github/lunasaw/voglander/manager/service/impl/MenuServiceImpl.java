package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luna.common.check.Assert;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.assembler.MenuAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import io.github.lunasaw.voglander.repository.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public List<MenuDTO> getUserMenus(Long userId) {
        List<MenuDO> menuDOList = userMapper.selectUserMenus(userId);
        return MenuAssembler.toDTOList(menuDOList);
    }

    @Override
    public List<MenuDTO> buildMenuTree(List<MenuDTO> menuList) {
        return MenuAssembler.buildMenuTree(menuList);
    }

    @Override
    public List<MenuDTO> getAllMenus() {
        LambdaQueryWrapper<MenuDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MenuDO::getStatus, 1)
            .orderByAsc(MenuDO::getSortOrder)
            .orderByAsc(MenuDO::getId);

        List<MenuDO> menuDOList = menuMapper.selectList(queryWrapper);
        return MenuAssembler.toDTOList(menuDOList);
    }

    @Override
    public MenuDTO getMenuById(Long id) {
        Assert.notNull(id, "菜单ID不能为空");

        MenuDO menuDO = menuMapper.selectById(id);
        return MenuAssembler.toDTO(menuDO);
    }

    @Override
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

        int result = menuMapper.insert(menuDO);
        if (result > 0) {
            log.info("创建菜单成功，菜单ID：{}", menuDO.getId());
            return menuDO.getId();
        } else {
            throw new ServiceException("创建菜单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(Long id, MenuDTO menuDTO) {
        Assert.notNull(id, "菜单ID不能为空");
        Assert.notNull(menuDTO, "菜单信息不能为空");

        MenuDO existMenu = menuMapper.selectById(id);
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

        int result = menuMapper.updateById(menuDO);
        if (result > 0) {
            log.info("更新菜单成功，菜单ID：{}", id);
            return true;
        } else {
            throw new ServiceException("更新菜单失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long id) {
        Assert.notNull(id, "菜单ID不能为空");

        MenuDO menuDO = menuMapper.selectById(id);
        if (menuDO == null) {
            throw new ServiceException("菜单不存在");
        }

        // 检查是否有子菜单
        LambdaQueryWrapper<MenuDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MenuDO::getParentId, id);
        long childCount = menuMapper.selectCount(queryWrapper);
        if (childCount > 0) {
            throw new ServiceException("该菜单下还有子菜单，无法删除");
        }

        int result = menuMapper.deleteById(id);
        if (result > 0) {
            log.info("删除菜单成功，菜单ID：{}", id);
            return true;
        } else {
            throw new ServiceException("删除菜单失败");
        }
    }

    @Override
    public boolean isMenuNameExists(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            return false;
        }

        LambdaQueryWrapper<MenuDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MenuDO::getMenuName, name);
        if (excludeId != null) {
            queryWrapper.ne(MenuDO::getId, excludeId);
        }

        return menuMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean isMenuPathExists(String path, Long excludeId) {
        if (StringUtils.isBlank(path)) {
            return false;
        }

        LambdaQueryWrapper<MenuDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MenuDO::getPath, path);
        if (excludeId != null) {
            queryWrapper.ne(MenuDO::getId, excludeId);
        }

        return menuMapper.selectCount(queryWrapper) > 0;
    }
}