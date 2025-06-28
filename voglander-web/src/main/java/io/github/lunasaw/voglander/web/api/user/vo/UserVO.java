package io.github.lunasaw.voglander.web.api.user.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

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
     * 最后登录时间
     */
    private LocalDateTime     lastLogin;

    /**
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 更新时间
     */
    private LocalDateTime     updateTime;
}