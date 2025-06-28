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
 * 角色实体
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_role")
public class RoleDO implements Serializable {

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
     * 角色名称
     */
    @TableField("role_name")
    private String            roleName;

    /**
     * 角色描述
     */
    @TableField("description")
    private String            description;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Integer           status;

    /**
     * 扩展字段
     */
    @TableField("extend")
    private String            extend;
}