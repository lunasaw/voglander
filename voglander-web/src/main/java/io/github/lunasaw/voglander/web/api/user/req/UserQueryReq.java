package io.github.lunasaw.voglander.web.api.user.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 用户查询请求
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class UserQueryReq implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 状态 1启用 0禁用
     */
    private Integer           status;

    /**
     * 页码
     */
    private Integer           pageNum          = 1;

    /**
     * 每页数量
     */
    private Integer           pageSize         = 10;
}