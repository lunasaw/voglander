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
 * 用户角色关联实体
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_user_role")
public class UserRoleDO implements Serializable {

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
     * 用户ID
     */
    @TableField("user_id")
    private Long              userId;

    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long              roleId;
}