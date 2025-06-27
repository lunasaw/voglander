package io.github.lunasaw.voglander.manager.domaon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单DTO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    private Long              id;

    /**
     * 父菜单ID
     */
    private Long              parentId;

    /**
     * 菜单编码
     */
    private String            menuCode;

    /**
     * 菜单名称
     */
    private String            menuName;

    /**
     * 菜单类型 1目录 2菜单 3按钮
     */
    private Integer           menuType;

    /**
     * 路由路径
     */
    private String            path;

    /**
     * 组件路径
     */
    private String            component;

    /**
     * 菜单图标
     */
    private String            icon;

    /**
     * 排序
     */
    private Integer           sortOrder;

    /**
     * 是否显示 1显示 0隐藏
     */
    private Integer           visible;

    /**
     * 状态 1启用 0禁用
     */
    private Integer           status;

    /**
     * 权限标识
     */
    private String            permission;

    /**
     * 激活时显示的图标
     */
    private String            activeIcon;

    /**
     * 子菜单列表
     */
    private List<MenuDTO>     children;
}