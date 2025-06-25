package io.github.lunasaw.voglander.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单实体
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_menu")
public class MenuDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long              id;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime     createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime     updateTime;

    /**
     * 父菜单ID
     */
    @TableField("parent_id")
    private Long              parentId;

    /**
     * 菜单编码
     */
    @TableField("menu_code")
    private String            menuCode;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    private String            menuName;

    /**
     * 菜单类型 1目录 2菜单 3按钮
     */
    @TableField("menu_type")
    private Integer           menuType;

    /**
     * 路由路径
     */
    @TableField("path")
    private String            path;

    /**
     * 组件路径
     */
    @TableField("component")
    private String            component;

    /**
     * 菜单图标
     */
    @TableField("icon")
    private String            icon;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer           sortOrder;

    /**
     * 是否显示 1显示 0隐藏
     */
    @TableField("visible")
    private Integer           visible;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Integer           status;

    /**
     * 权限标识
     */
    @TableField("permission")
    private String            permission;

    /**
     * 扩展字段
     */
    @TableField("extend")
    private String            extend;
}