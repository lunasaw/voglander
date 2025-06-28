package io.github.lunasaw.voglander.web.api.user.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 用户列表响应VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserListResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户列表
     */
    private List<UserVO>      items;
}