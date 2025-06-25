package io.github.lunasaw.voglander.manager.domaon.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 菜单VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 路由路径
     */
    private String            path;

    /**
     * 路由名称
     */
    private String            name;

    /**
     * 组件路径
     */
    private String            component;

    /**
     * 重定向
     */
    private String            redirect;

    /**
     * 路由元数据
     */
    private Meta              meta;

    /**
     * 子路由
     */
    private List<MenuVO>      children;

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Meta implements Serializable {
        private static final long   serialVersionUID = 1L;

        /**
         * 菜单和面包屑对应的图标
         */
        private String              icon;

        /**
         * 路由标题 (用作 document.title || 菜单的名称)
         */
        private String              title;

        /**
         * 菜单排序，数字越小越靠前
         */
        private Integer             order;

        /**
         * 是否固定在标签页中
         */
        private Boolean             affixTab;

        /**
         * 是否开启页面缓存
         */
        private Boolean             keepAlive;

        /**
         * 页面权限，不满足权限不显示菜单
         */
        private List<String>        authority;

        /**
         * 徽章文本
         */
        private String              badge;

        /**
         * 徽章类型
         */
        private String              badgeType;

        /**
         * 徽章变体
         */
        private String              badgeVariants;

        /**
         * iframe页面地址
         */
        private String              iframeSrc;

        /**
         * 外链地址
         */
        private String              link;

        /**
         * 是否在菜单中隐藏
         */
        private Boolean             hideInMenu;

        /**
         * 当前激活的菜单
         */
        private String              currentActiveMenu;

        /**
         * 指定该路由切换的动画名
         */
        private String              transitionName;

        /**
         * 额外的属性
         */
        private Map<String, Object> extra;
    }
}