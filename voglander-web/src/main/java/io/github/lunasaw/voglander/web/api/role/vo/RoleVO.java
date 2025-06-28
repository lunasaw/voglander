package io.github.lunasaw.voglander.web.api.role.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色视图对象
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long              id;

    /**
     * 角色名称
     */
    private String            name;

    /**
     * 角色描述
     */
    private String            remark;

    /**
     * 状态 1启用 0禁用
     */
    private Integer           status;

    /**
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 更新时间
     */
    private LocalDateTime     updateTime;

    /**
     * 权限列表
     */
    private List<Long>        permissions      = new ArrayList<>();
}
