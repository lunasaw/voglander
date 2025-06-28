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
 * 用户实体
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_user")
public class UserDO implements Serializable {

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
     * 用户名
     */
    @TableField("username")
    private String            username;

    /**
     * 密码
     */
    @TableField("password")
    private String            password;

    /**
     * 昵称
     */
    @TableField("nickname")
    private String            nickname;

    /**
     * 邮箱
     */
    @TableField("email")
    private String            email;

    /**
     * 手机号
     */
    @TableField("phone")
    private String            phone;

    /**
     * 头像URL
     */
    @TableField("avatar")
    private String            avatar;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Integer           status;

    /**
     * 最后登录时间
     */
    @TableField("last_login")
    private LocalDateTime     lastLogin;

    /**
     * 扩展字段
     */
    @TableField("extend")
    private String            extend;
}