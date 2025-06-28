package io.github.lunasaw.voglander.web.api.user.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoVO implements Serializable {

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
     * 真实姓名
     */
    private String            realName;

    /**
     * 头像
     */
    private String            avatar;

    /**
     * 描述
     */
    private String            desc;

    /**
     * 首页路径
     */
    private String            homePath;

    /**
     * 角色列表 - 简化为字符串数组以匹配前端期望
     */
    private List<String>      roles;
}