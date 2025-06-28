package io.github.lunasaw.voglander.web.api.role.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 角色查询请求
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RoleQueryReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    private String            name;

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