package io.github.lunasaw.voglander.web.api.user.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 用户创建请求
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名只能包含字母、数字、下划线，长度4-20位")
    private String            username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
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

    /**
     * 角色ID列表（可选）
     */
    private List<Long>        roleIds;
}