package io.github.lunasaw.voglander.manager.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.domaon.vo.MenuVO;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单数据转换器
 *
 * @author luna
 */
public class MenuAssembler {

    /**
     * DO转DTO
     */
    public static MenuDTO toDTO(MenuDO menuDO) {
        if (menuDO == null) {
            return null;
        }
        MenuDTO dto = new MenuDTO();
        dto.setId(menuDO.getId());
        dto.setParentId(menuDO.getParentId());
        dto.setMenuCode(menuDO.getMenuCode());
        dto.setMenuName(menuDO.getMenuName());
        dto.setMenuType(menuDO.getMenuType());
        dto.setPath(menuDO.getPath());
        dto.setComponent(menuDO.getComponent());
        dto.setIcon(menuDO.getIcon());
        dto.setSortOrder(menuDO.getSortOrder());
        dto.setVisible(menuDO.getVisible());
        dto.setStatus(menuDO.getStatus());
        dto.setPermission(menuDO.getPermission());
        dto.setActiveIcon(menuDO.getActiveIcon());
        return dto;
    }

    /**
     * DO列表转DTO列表
     */
    public static List<MenuDTO> toDTOList(List<MenuDO> menuDOList) {
        if (menuDOList == null || menuDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return menuDOList.stream().map(MenuAssembler::toDTO).collect(Collectors.toList());
    }

    /**
     * 构建菜单树
     */
    public static List<MenuDTO> buildMenuTree(List<MenuDTO> menuList) {
        if (menuList == null || menuList.isEmpty()) {
            return Collections.emptyList();
        }

        // 按父ID分组
        Map<Long, List<MenuDTO>> parentMenuMap = menuList.stream()
            .collect(Collectors.groupingBy(MenuDTO::getParentId));

        // 找出根菜单并构建树
        List<MenuDTO> rootMenus = parentMenuMap.getOrDefault(0L, Collections.emptyList());
        buildChildren(rootMenus, parentMenuMap);

        return rootMenus;
    }

    /**
     * 递归构建子菜单
     */
    private static void buildChildren(List<MenuDTO> parentMenus, Map<Long, List<MenuDTO>> parentMenuMap) {
        if (parentMenus == null || parentMenus.isEmpty()) {
            return;
        }

        for (MenuDTO parentMenu : parentMenus) {
            List<MenuDTO> children = parentMenuMap.getOrDefault(parentMenu.getId(), Collections.emptyList());
            if (!children.isEmpty()) {
                parentMenu.setChildren(children);
                buildChildren(children, parentMenuMap);
            }
        }
    }

    /**
     * DTO转VO (前端路由格式)
     */
    public static MenuVO toVO(MenuDTO menuDTO) {
        if (menuDTO == null) {
            return null;
        }

        MenuVO vo = new MenuVO();
        vo.setPath(StringUtils.isNotBlank(menuDTO.getPath()) ? menuDTO.getPath() : "/" + menuDTO.getMenuCode().toLowerCase());
        vo.setName(menuDTO.getMenuCode());
        vo.setComponent(StringUtils.isNotBlank(menuDTO.getComponent()) ? menuDTO.getComponent() : "");

        // 设置元数据
        MenuVO.Meta meta = new MenuVO.Meta();
        meta.setIcon(menuDTO.getIcon());
        meta.setTitle(menuDTO.getMenuName());
        meta.setOrder(menuDTO.getSortOrder() != null ? menuDTO.getSortOrder().intValue() : 0);
        meta.setHideInMenu(menuDTO.getVisible() == 0);
        meta.setAffixTab(false); // 默认不固定
        meta.setKeepAlive(true); // 默认开启缓存

        if (StringUtils.isNotBlank(menuDTO.getPermission())) {
            meta.setAuthority(Collections.singletonList(menuDTO.getPermission()));
        }

        vo.setMeta(meta);

        // 处理子菜单
        if (menuDTO.getChildren() != null && !menuDTO.getChildren().isEmpty()) {
            List<MenuVO> children = new ArrayList<>();
            for (MenuDTO child : menuDTO.getChildren()) {
                MenuVO childVO = toVO(child);
                if (childVO != null) {
                    children.add(childVO);
                }
            }
            vo.setChildren(children);
        }

        return vo;
    }

    /**
     * DTO列表转VO列表
     */
    public static List<MenuVO> toVOList(List<MenuDTO> menuDTOList) {
        if (menuDTOList == null || menuDTOList.isEmpty()) {
            return Collections.emptyList();
        }
        return menuDTOList.stream().map(MenuAssembler::toVO).collect(Collectors.toList());
    }

    /**
     * DTO转DO
     */
    public static MenuDO toDO(MenuDTO menuDTO) {
        if (menuDTO == null) {
            return null;
        }
        MenuDO menuDO = new MenuDO();
        menuDO.setId(menuDTO.getId());
        menuDO.setParentId(menuDTO.getParentId());
        menuDO.setMenuCode(menuDTO.getMenuCode());
        menuDO.setMenuName(menuDTO.getMenuName());
        menuDO.setMenuType(menuDTO.getMenuType());
        menuDO.setPath(menuDTO.getPath());
        menuDO.setComponent(menuDTO.getComponent());
        menuDO.setIcon(menuDTO.getIcon());
        menuDO.setSortOrder(menuDTO.getSortOrder());
        menuDO.setVisible(menuDTO.getVisible());
        menuDO.setStatus(menuDTO.getStatus());
        menuDO.setPermission(menuDTO.getPermission());
        menuDO.setActiveIcon(menuDTO.getActiveIcon());
        return menuDO;
    }
}