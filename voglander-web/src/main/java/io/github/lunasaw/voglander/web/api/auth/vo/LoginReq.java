package io.github.lunasaw.voglander.web.api.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 登录请求VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LoginReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String            username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String            password;
}