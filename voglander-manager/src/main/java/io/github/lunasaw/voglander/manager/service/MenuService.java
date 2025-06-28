package io.github.lunasaw.voglander.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.repository.entity.MenuDO;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author luna
 */
public interface MenuService extends IService<MenuDO> {

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

    /**
     * 获取所有菜单列表
     *
     * @return 菜单列表
     */
    List<MenuDTO> getAllMenus();

    /**
     * 根据ID获取菜单
     *
     * @param id 菜单ID
     * @return 菜单信息
     */
    MenuDTO getMenuById(Long id);

    /**
     * 创建菜单
     *
     * @param menuDTO 菜单信息
     * @return 创建的菜单ID
     */
    Long createMenu(MenuDTO menuDTO);

    /**
     * 更新菜单
     *
     * @param id 菜单ID
     * @param menuDTO 菜单信息
     * @return 是否更新成功
     */
    boolean updateMenu(Long id, MenuDTO menuDTO);

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return 是否删除成功
     */
    boolean deleteMenu(Long id);

    /**
     * 检查菜单名称是否存在
     *
     * @param name 菜单名称
     * @param excludeId 排除的菜单ID
     * @return 是否存在
     */
    boolean isMenuNameExists(String name, Long excludeId);

    /**
     * 检查菜单路径是否存在
     *
     * @param path 菜单路径
     * @param excludeId 排除的菜单ID
     * @return 是否存在
     */
    boolean isMenuPathExists(String path, Long excludeId);
}
