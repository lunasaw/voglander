package io.github.lunasaw.voglander.manager.domaon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 部门请求DTO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DeptReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空")
    private String            name;

    /**
     * 部门描述
     */
    private String            remark;

    /**
     * 状态 1启用 0禁用
     */
    @NotNull(message = "状态不能为空")
    private Integer           status;

    /**
     * 父部门ID
     */
    private String            parentId;

    /**
     * 排序
     */
    private Integer           sortOrder;

    /**
     * 部门负责人
     */
    private String            leader;

    /**
     * 联系电话
     */
    private String            phone;

    /**
     * 邮箱
     */
    private String            email;
}