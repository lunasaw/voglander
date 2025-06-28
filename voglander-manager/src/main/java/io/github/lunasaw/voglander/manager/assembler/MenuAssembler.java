package io.github.lunasaw.voglander.manager.assembler;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuMeta;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜单数据转换器
 *
 * @author luna
 */
@Slf4j
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

        // 处理meta字段 - JSON反序列化
        if (StringUtils.isNotBlank(menuDO.getMeta())) {
            MenuMeta menuMeta = JSON.parseObject(menuDO.getMeta(), MenuMeta.class);
            dto.setMeta(menuMeta);
        } else {
            dto.setMeta(new MenuMeta());
        }

        return dto;
    }

    /**
     * DO列表转DTO列表
     */
    public static List<MenuDTO> toDTOList(List<MenuDO> menuDOList) {
        if (menuDOList == null || menuDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return menuDOList.stream().map(MenuAssembler::toDTO).filter(Objects::nonNull).collect(Collectors.toList());
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

        // 处理meta字段 - JSON序列化
        if (menuDTO.getMeta() != null) {
            String metaJson = JSON.toJSONString(menuDTO.getMeta());
            menuDO.setMeta(metaJson);
        }

        return menuDO;
    }
}