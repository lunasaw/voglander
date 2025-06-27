package io.github.lunasaw.voglander.web.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.MenuDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuMeta;
import io.github.lunasaw.voglander.manager.domaon.dto.MenuReq;
import io.github.lunasaw.voglander.manager.domaon.vo.MenuResp;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单Web层转换器
 * 负责 Req -> DTO 和 DTO -> Resp 的转换
 *
 * @author luna
 */
@Component
public class MenuWebAssembler {

    /**
     * MenuReq转换为MenuDTO
     *
     * @param menuReq 菜单请求对象
     * @return MenuDTO
     */
    public MenuDTO toDTO(MenuReq menuReq) {
        if (menuReq == null) {
            return null;
        }

        MenuDTO dto = new MenuDTO();
        dto.setMenuCode(menuReq.getAuthCode());
        dto.setMenuName(menuReq.getName());
        dto.setPath(menuReq.getPath());
        dto.setComponent(menuReq.getComponent());
        dto.setPermission(menuReq.getAuthCode());

        // 转换父级ID
        if (StringUtils.isNotBlank(menuReq.getPid())) {
            try {
                dto.setParentId(Long.valueOf(menuReq.getPid()));
            } catch (NumberFormatException e) {
                dto.setParentId(0L); // 默认为根节点
            }
        } else {
            dto.setParentId(0L);
        }

        // 转换菜单类型
        dto.setMenuType(convertMenuType(menuReq.getType()));

        // 转换元数据
        MenuMeta meta = new MenuMeta();
        if (menuReq.getMeta() != null) {
            MenuReq.MetaReq metaReq = menuReq.getMeta();

            dto.setIcon(metaReq.getIcon());
            dto.setSortOrder(metaReq.getOrder());
            dto.setVisible(metaReq.getHideInMenu() != null && metaReq.getHideInMenu() ? 0 : 1);

            // 设置meta数据
            meta.setActiveIcon(metaReq.getActiveIcon());
            meta.setActivePath(metaReq.getActivePath());
            meta.setAffixTab(metaReq.getAffixTab());
            meta.setAffixTabOrder(metaReq.getAffixTabOrder());
            meta.setBadge(metaReq.getBadge());
            meta.setBadgeType(metaReq.getBadgeType());
            meta.setBadgeVariants(metaReq.getBadgeVariants());
            meta.setHideChildrenInMenu(metaReq.getHideChildrenInMenu());
            meta.setHideInBreadcrumb(metaReq.getHideInBreadcrumb());
            meta.setHideInMenu(metaReq.getHideInMenu());
            meta.setHideInTab(metaReq.getHideInTab());
            meta.setIframeSrc(metaReq.getIframeSrc());
            meta.setKeepAlive(metaReq.getKeepAlive());
            meta.setLink(metaReq.getLink());
            meta.setMaxNumOfOpenTab(metaReq.getMaxNumOfOpenTab());
            meta.setNoBasicLayout(metaReq.getNoBasicLayout());
            meta.setOpenInNewWindow(metaReq.getOpenInNewWindow());
            meta.setOrder(metaReq.getOrder());
            meta.setTitle(metaReq.getTitle());
        }
        dto.setMeta(meta);

        // 默认启用状态
        dto.setStatus(1);

        return dto;
    }

    /**
     * MenuDTO转换为MenuResp
     *
     * @param menuDTO 菜单DTO对象
     * @return MenuResp
     */
    public MenuResp toResp(MenuDTO menuDTO) {
        if (menuDTO == null) {
            return null;
        }

        MenuResp resp = new MenuResp();
        resp.setId(menuDTO.getId());
        resp.setAuthCode(menuDTO.getMenuCode());
        resp.setName(menuDTO.getMenuName());
        resp.setPath(menuDTO.getPath());
        resp.setComponent(menuDTO.getComponent());
        resp.setPid(menuDTO.getParentId() != null ? menuDTO.getParentId() : 0L);
        resp.setType(convertMenuTypeToString(menuDTO.getMenuType()));
        resp.setStatus(menuDTO.getStatus());

        // 转换元数据
        MenuResp.MetaResp meta = new MenuResp.MetaResp();
        meta.setIcon(menuDTO.getIcon());
        meta.setOrder(menuDTO.getSortOrder());
        meta.setTitle(menuDTO.getMenuName());
        meta.setHideInMenu(menuDTO.getVisible() == 0);
        meta.setKeepAlive(true);
        meta.setAffixTab(false);

        // 从MenuMeta中获取详细信息
        if (menuDTO.getMeta() != null) {
            MenuMeta menuMeta = menuDTO.getMeta();
            meta.setActiveIcon(menuMeta.getActiveIcon());
            meta.setActivePath(menuMeta.getActivePath());
            meta.setAffixTab(menuMeta.getAffixTab());
            meta.setAffixTabOrder(menuMeta.getAffixTabOrder());
            meta.setBadge(menuMeta.getBadge());
            meta.setBadgeType(menuMeta.getBadgeType());
            meta.setBadgeVariants(menuMeta.getBadgeVariants());
            meta.setHideChildrenInMenu(menuMeta.getHideChildrenInMenu());
            meta.setHideInBreadcrumb(menuMeta.getHideInBreadcrumb());
            meta.setHideInMenu(menuMeta.getHideInMenu());
            meta.setHideInTab(menuMeta.getHideInTab());
            meta.setIframeSrc(menuMeta.getIframeSrc());
            meta.setKeepAlive(menuMeta.getKeepAlive());
            meta.setLink(menuMeta.getLink());
            meta.setMaxNumOfOpenTab(menuMeta.getMaxNumOfOpenTab());
            meta.setNoBasicLayout(menuMeta.getNoBasicLayout());
            meta.setOpenInNewWindow(menuMeta.getOpenInNewWindow());
            meta.setOrder(menuMeta.getOrder());
            meta.setTitle(menuMeta.getTitle());

            // 如果meta中有覆盖值，使用meta中的值
            if (menuMeta.getOrder() != null) {
                meta.setOrder(menuMeta.getOrder());
            }
            if (menuMeta.getHideInMenu() != null) {
                meta.setHideInMenu(menuMeta.getHideInMenu());
            }
            if (menuMeta.getKeepAlive() != null) {
                meta.setKeepAlive(menuMeta.getKeepAlive());
            }
            if (menuMeta.getAffixTab() != null) {
                meta.setAffixTab(menuMeta.getAffixTab());
            }
            if (StringUtils.isNotBlank(menuMeta.getTitle())) {
                meta.setTitle(menuMeta.getTitle());
            }
        }

        // 设置徽标类型，根据菜单类型设置默认值
        if (menuDTO.getMenuType() != null && menuDTO.getMenuType() == 1) {
            // 目录类型设置点徽标
            if (StringUtils.isBlank(meta.getBadgeType())) {
                meta.setBadgeType("dot");
            }
        }

        resp.setMeta(meta);

        // 转换子菜单
        if (menuDTO.getChildren() != null && !menuDTO.getChildren().isEmpty()) {
            List<MenuResp> children = menuDTO.getChildren().stream()
                .map(this::toResp)
                .collect(Collectors.toList());
            resp.setChildren(children);
        }

        return resp;
    }

    /**
     * MenuDTO列表转换为MenuResp列表
     *
     * @param menuDTOList 菜单DTO列表
     * @return MenuResp列表
     */
    public List<MenuResp> toRespList(List<MenuDTO> menuDTOList) {
        if (menuDTOList == null || menuDTOList.isEmpty()) {
            return Collections.emptyList();
        }
        return menuDTOList.stream().map(this::toResp).collect(Collectors.toList());
    }

    /**
     * 转换菜单类型字符串为数字
     *
     * @param typeStr 类型字符串
     * @return 类型数字
     */
    private Integer convertMenuType(String typeStr) {
        if (StringUtils.isBlank(typeStr)) {
            return 2; // 默认为菜单
        }
        switch (typeStr.toLowerCase()) {
            case "catalog":
                return 1; // 目录
            case "menu":
                return 2; // 菜单
            case "button":
                return 3; // 按钮
            case "embedded":
                return 4; // 内嵌
            case "link":
                return 5; // 外链
            default:
                return 2; // 默认为菜单
        }
    }

    /**
     * 转换菜单类型数字为字符串
     *
     * @param typeNum 类型数字
     * @return 类型字符串
     */
    private String convertMenuTypeToString(Integer typeNum) {
        if (typeNum == null) {
            return "menu";
        }
        switch (typeNum) {
            case 1:
                return "catalog";
            case 2:
                return "menu";
            case 3:
                return "button";
            case 4:
                return "embedded";
            case 5:
                return "link";
            default:
                return "menu";
        }
    }
}