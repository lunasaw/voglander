package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.manager.MenuManager;
import io.github.lunasaw.voglander.manager.service.MenuService;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import io.github.lunasaw.voglander.repository.mapper.MenuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单服务实现类
 *
 * @author luna
 */
@Slf4j
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, MenuDO> implements MenuService {

    @Autowired
    private MenuManager menuManager;

    @Override
    public List<MenuDTO> getUserMenus(Long userId) {
        return menuManager.getUserMenus(userId);
    }

    @Override
    public List<MenuDTO> buildMenuTree(List<MenuDTO> menuList) {
        return menuManager.buildMenuTree(menuList);
    }

    @Override
    public List<MenuDTO> getAllMenus() {
        return menuManager.getAllMenus();
    }

    @Override
    public MenuDTO getMenuById(Long id) {
        return menuManager.getMenuById(id);
    }

    @Override
    public Long createMenu(MenuDTO menuDTO) {
        return menuManager.createMenu(menuDTO);
    }

    @Override
    public boolean updateMenu(Long id, MenuDTO menuDTO) {
        return menuManager.updateMenu(id, menuDTO);
    }

    @Override
    public boolean deleteMenu(Long id) {
        return menuManager.deleteMenu(id);
    }

    @Override
    public boolean isMenuNameExists(String name, Long excludeId) {
        return menuManager.isMenuNameExists(name, excludeId);
    }

    @Override
    public boolean isMenuPathExists(String path, Long excludeId) {
        return menuManager.isMenuPathExists(path, excludeId);
    }
}