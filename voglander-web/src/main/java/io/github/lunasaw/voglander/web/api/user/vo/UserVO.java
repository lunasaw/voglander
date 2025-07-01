package io.github.lunasaw.voglander.web.api.user.vo;

import io.github.lunasaw.voglander.web.api.role.vo.RoleVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 用户响应VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserVO implements Serializable {

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
     * 最后登录时间 (unix时间戳，毫秒级)
     */
    private Long              lastLoginTime;

    /**
     * 创建时间 (unix时间戳，毫秒级)
     */
    private Long              createTime;

    /**
     * 更新时间 (unix时间戳，毫秒级)
     */
    private Long              updateTime;

    /**
     * 角色ID列表
     */
    private List<Long>        roleIds;

    /**
     * 角色信息列表
     */
    private List<RoleVO>      roles;
}