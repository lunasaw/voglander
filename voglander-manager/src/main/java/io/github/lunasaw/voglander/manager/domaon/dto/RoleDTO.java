package io.github.lunasaw.voglander.manager.domaon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色数据传输对象
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long              id;

    /**
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 更新时间
     */
    private LocalDateTime     updateTime;

    /**
     * 角色名称
     */
    private String            roleName;

    /**
     * 角色描述
     */
    private String            description;

    /**
     * 状态 1启用 0禁用
     */
    private Integer           status;

    /**
     * 扩展字段
     */
    private String            extend;

    /**
     * 权限列表
     */
    private List<Long>        permissions;

    /**
     * 页码
     */
    private Integer           pageNum;

    /**
     * 每页数量
     */
    private Integer           pageSize;
}