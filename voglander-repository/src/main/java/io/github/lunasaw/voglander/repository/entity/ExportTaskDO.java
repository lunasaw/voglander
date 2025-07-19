package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.github.lunasaw.voglander.common.enums.export.ExportTaskTypeEnums;
import io.github.lunasaw.voglander.common.enums.export.ExportTaskStatusEnum;
import lombok.Data;

/**
 * (ExportTask)实体类
 *
 * @author chenzhangyue
 * @since 2024-01-26 15:21:45
 */
@Data
@TableName("tb_export_task")
public class ExportTaskDO implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * ID自增
     */
    @TableId(type = IdType.AUTO)
    private Long              id;
    /**
     * 创建时间
     */
    private LocalDateTime     gmtCreate;
    /**
     * 更新时间
     */
    private LocalDateTime     gmtUpdate;
    /**
     * 任务唯一Id
     */
    private Long              bizId;
    /**
     * 导出的客户总数
     */
    private Long              memberCnt;
    /**
     * 文件格式
     */
    private String            format;
    /**
     * 申请时间
     */
    private LocalDateTime     applyTime;
    /**
     * 导出报表时间
     */
    private LocalDateTime     exportTime;
    /**
     * 文件下载地址, 多个url用、隔开
     */
    private String            url;
    /**
     * {@link ExportTaskStatusEnum}
     * 是否完成，1 -> 完成, 0->处理中, -1 -> 出错
     */
    private Integer           status;
    /**
     * 是否过期，1 -> 过期，0 -> 未过期
     */
    private Integer           expired;
    /**
     * 是否删除，1 -> 删除, 0 -> 未删除
     */
    private Integer           deleted;
    /**
     * 搜索条件序列化
     */
    private String            param;

    /**
     * 导出名称
     */
    private String            name;

    /**
     * {@link ExportTaskTypeEnums}
     * 导出类型
     */
    private Integer           type;

    private String            applyUser;

    private String            extend;
}
