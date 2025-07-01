package io.github.lunasaw.voglander.web.api.dept.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 部门响应VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DeptResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private String            id;

    /**
     * 部门名称
     */
    private String            name;

    /**
     * 部门描述
     */
    private String            remark;

    /**
     * 状态 1启用 0禁用
     */
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

    /**
     * 创建时间 (unix时间戳，毫秒级)
     */
    private Long              createTime;

    /**
     * 子部门列表
     */
    private List<DeptResp>    children;
}