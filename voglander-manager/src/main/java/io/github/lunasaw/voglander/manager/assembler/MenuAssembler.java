package io.github.lunasaw.voglander.manager.assembler;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuMeta;
import io.github.lunasaw.voglander.manager.domaon.vo.MenuVO;
import io.github.lunasaw.voglander.repository.entity.MenuDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

        // 使用fastjson2进行对象转换，大大简化代码
        String jsonString = JSON.toJSONString(menuDTO);
        MenuVO vo = JSON.parseObject(jsonString, MenuVO.class);

        // 处理特殊字段映射和业务逻辑
        vo.setPath(StringUtils.isNotBlank(menuDTO.getPath()) ? menuDTO.getPath() : "/" + menuDTO.getMenuCode().toLowerCase());
        vo.setName(menuDTO.getMenuCode());
        vo.setComponent(StringUtils.isNotBlank(menuDTO.getComponent()) ? menuDTO.getComponent() : "");
        vo.setRedirect(null); // 默认重定向为null

        // 处理meta字段的特殊逻辑
        if (vo.getMeta() == null) {
            vo.setMeta(new MenuVO.Meta());
        }
        MenuVO.Meta meta = vo.getMeta();

        // 设置基础元数据
        meta.setIcon(StringUtils.isNotBlank(menuDTO.getIcon()) ? menuDTO.getIcon() : "");
        meta.setTitle(menuDTO.getMenuName());
        meta.setOrder(menuDTO.getSortOrder() != null ? menuDTO.getSortOrder().intValue() : 0);
        meta.setHideInMenu(menuDTO.getVisible() == 0);

        // 设置默认值
        if (meta.getAffixTab() == null) {
            meta.setAffixTab(false);
        }
        if (meta.getKeepAlive() == null) {
            meta.setKeepAlive(true);
        }

        // 处理权限
        if (StringUtils.isNotBlank(menuDTO.getPermission())) {
            meta.setAuthority(Collections.singletonList(menuDTO.getPermission()));
        }

        // 处理子菜单递归转换
        if (menuDTO.getChildren() != null && !menuDTO.getChildren().isEmpty()) {
            List<MenuVO> children = menuDTO.getChildren().stream()
                .map(MenuAssembler::toVO)
                .filter(child -> child != null)
                .collect(Collectors.toList());
            vo.setChildren(!children.isEmpty() ? children : null);
        } else {
            vo.setChildren(null);
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

        // 处理meta字段 - JSON序列化
        if (menuDTO.getMeta() != null) {
            String metaJson = JSON.toJSONString(menuDTO.getMeta());
            menuDO.setMeta(metaJson);
        }

        return menuDO;
    }
}