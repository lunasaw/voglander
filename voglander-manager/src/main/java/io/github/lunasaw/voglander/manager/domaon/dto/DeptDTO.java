package io.github.lunasaw.voglander.manager.domaon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 部门DTO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DeptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long              id;

    /**
     * 父部门ID
     */
    private Long              parentId;

    /**
     * 部门名称
     */
    private String            deptName;

    /**
     * 部门编码
     */
    private String            deptCode;

    /**
     * 部门描述
     */
    private String            remark;

    /**
     * 状态 1启用 0禁用
     */
    private Integer           status;

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
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 子部门列表
     */
    private List<DeptDTO>     children;

    // ================ 时间转换领域方法 ================

    /**
     * 获取创建时间的毫秒级时间戳
     *
     * @return unix时间戳（毫秒级）
     */
    public Long createTimeToEpochMilli() {
        return createTime != null ? createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null;
    }
}