package io.github.lunasaw.voglander.manager.domaon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 菜单请求DTO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MenuReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 后端权限标识
     */
    @NotBlank(message = "权限标识不能为空")
    private String            authCode;

    /**
     * 组件路径
     */
    private String            component;

    /**
     * 菜单名称
     */
    @NotBlank(message = "菜单名称不能为空")
    private String            name;

    /**
     * 路由路径
     */
    @NotBlank(message = "路由路径不能为空")
    private String            path;

    /**
     * 父级ID
     */
    @NotBlank(message = "父级ID不能为空")
    private String            pid;

    /**
     * 重定向
     */
    private String            redirect;

    /**
     * 菜单类型
     */
    @NotBlank(message = "菜单类型不能为空")
    private String            type;

    /**
     * 菜单元数据
     */
    private MetaReq           meta;

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MetaReq implements Serializable {
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