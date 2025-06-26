package io.github.lunasaw.voglander.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门实体
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_dept")
public class DeptDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long              id;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime     createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime     updateTime;

    /**
     * 父部门ID
     */
    @TableField("parent_id")
    private Long              parentId;

    /**
     * 部门名称
     */
    @TableField("dept_name")
    private String            deptName;

    /**
     * 部门编码
     */
    @TableField("dept_code")
    private String            deptCode;

    /**
     * 部门描述
     */
    @TableField("remark")
    private String            remark;

    /**
     * 状态 1启用 0禁用
     */
    @TableField("status")
    private Integer           status;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer           sortOrder;

    /**
     * 部门负责人
     */
    @TableField("leader")
    private String            leader;

    /**
     * 联系电话
     */
    @TableField("phone")
    private String            phone;

    /**
     * 邮箱
     */
    @TableField("email")
    private String            email;

    /**
     * 扩展字段
     */
    @TableField("extend")
    private String            extend;
}