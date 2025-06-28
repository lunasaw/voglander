package io.github.lunasaw.voglander.web.api.user.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 用户更新请求
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long              id;

    /**
     * 密码（可选，不传则不更新密码）
     */
    @Pattern(regexp = "^.{6,20}$", message = "密码长度必须在6-20位之间")
    private String            password;

    /**
     * 昵称
     */
    private String            nickname;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String            email;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String            phone;

    /**
     * 头像URL
     */
    private String            avatar;

    /**
     * 状态 1启用 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer           status;
}