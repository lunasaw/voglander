package io.github.lunasaw.voglander.manager.service;

import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author luna
 */
public interface MenuService {

    /**
     * 根据用户ID获取用户菜单
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<MenuDTO> getUserMenus(Long userId);

    /**
     * 构建菜单树
     *
     * @param menuList 菜单列表
     * @return 菜单树
     */
    List<MenuDTO> buildMenuTree(List<MenuDTO> menuList);
}
