package io.github.lunasaw.voglander.manager.domaon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 用户DTO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long              id;

    /**
     * 用户名
     */
    private String            username;

    /**
     * 密码
     */
    private String            password;

    /**
     * 昵称
     */
    private String            nickname;

    /**
     * 邮箱
     */
    private String            email;

    /**
     * 手机号
     */
    private String            phone;

    /**
     * 头像URL
     */
    private String            avatar;

    /**
     * 状态 1启用 0禁用
     */
    private Integer           status;

    /**
     * 最后登录时间
     */
    private LocalDateTime     lastLogin;

    /**
     * 权限码列表
     */
    private List<String>      permissions;

    /**
     * 角色ID列表
     */
    private List<Long>        roleIds;

    /**
     * 角色信息列表
     */
    private List<RoleDTO>     roles;

    /**
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 更新时间
     */
    private LocalDateTime     updateTime;

    /**
     * 页码
     */
    private Integer           pageNum;

    /**
     * 每页数量
     */
    private Integer           pageSize;

    // ================ 时间转换领域方法 ================

    /**
     * 获取最后登录时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long lastLoginToEpochMilli() {
        return lastLogin != null ? lastLogin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    /**
     * 获取创建时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long createTimeToEpochMilli() {
        return createTime != null ? createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }

    /**
     * 获取更新时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long updateTimeToEpochMilli() {
        return updateTime != null ? updateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }
}