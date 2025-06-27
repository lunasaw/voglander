package io.github.lunasaw.voglander.manager.domaon.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单响应VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MenuResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 后端权限标识
     */
    private String            authCode;

    /**
     * 子级
     */
    private List<MenuResp>    children;

    /**
     * 组件
     */
    private String            component;

    /**
     * 菜单ID
     */
    private Long              id;

    /**
     * 菜单元数据
     */
    private MetaResp          meta;

    /**
     * 菜单名称
     */
    private String            name;

    /**
     * 路由路径
     */
    private String            path;

    /**
     * 父级ID
     */
    private Long              pid;

    /**
     * 重定向
     */
    private String            redirect;

    /**
     * 菜单类型
     */
    private String            type;

    /**
     * 菜单状态 1启用 0禁用
     */
    private Integer           status;

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetaResp implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 激活时显示的图标
         */
        private String            activeIcon;

        /**
         * 作为路由时，需要激活的菜单的Path
         */
        private String            activePath;

        /**
         * 固定在标签栏
         */
        private Boolean           affixTab;

        /**
         * 在标签栏固定的顺序
         */
        private Integer           affixTabOrder;

        /**
         * 徽标内容
         */
        private String            badge;

        /**
         * 徽标类型
         */
        private String            badgeType;

        /**
         * 徽标颜色
         */
        private String            badgeVariants;

        /**
         * 在菜单中隐藏下级
         */
        private Boolean           hideChildrenInMenu;

        /**
         * 在面包屑中隐藏
         */
        private Boolean           hideInBreadcrumb;

        /**
         * 在菜单中隐藏
         */
        private Boolean           hideInMenu;

        /**
         * 在标签栏中隐藏
         */
        private Boolean           hideInTab;

        /**
         * 菜单图标
         */
        private String            icon;

        /**
         * 内嵌Iframe的URL
         */
        private String            iframeSrc;

        /**
         * 是否缓存页面
         */
        private Boolean           keepAlive;

        /**
         * 外链页面的URL
         */
        private String            link;

        /**
         * 同一个路由最大打开的标签数
         */
        private Integer           maxNumOfOpenTab;

        /**
         * 无需基础布局
         */
        private Boolean           noBasicLayout;

        /**
         * 是否在新窗口打开
         */
        private Boolean           openInNewWindow;

        /**
         * 菜单排序
         */
        private Integer           order;

        /**
         * 菜单标题
         */
        private String            title;
    }
}