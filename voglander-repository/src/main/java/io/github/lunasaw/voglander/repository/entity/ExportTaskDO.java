package io.github.lunasaw.voglander.repository.entity;

import java.util.Date;

import lombok.Data;

/**
 * (ExportTask)实体类
 *
 * @author chenzhangyue
 * @since 2024-01-26 15:21:45
 */
@Data
public class ExportTaskDO {

    /**
     * ID自增
     */
    private Long    id;
    /**
     * 创建时间
     */
    private Date    gmtCreate;
    /**
     * 更新时间
     */
    private Date    gmtUpdate;
    /**
     * 任务唯一Id
     */
    private Long    bizId;
    /**
     * 导出的客户总数
     */
    private Long    memberCnt;
    /**
     * 文件格式
     */
    private String  format;
    /**
     * 申请时间
     */
    private Date    applyTime;
    /**
     * 导出报表时间
     */
    private Date    exportTime;
    /**
     * 文件下载地址, 多个url用、隔开
     */
    private String  url;
    /**
     * 是否完成，1 -> 完成, 0->处理中, -1 -> 出错
     */
    private Integer status;
    /**
     * 是否过期，1 -> 过期，0 -> 未过期
     */
    private Integer expired;
    /**
     * 是否删除，1 -> 删除, 0 -> 未删除
     */
    private Integer deleted;
    /**
     * 搜索条件序列化
     */
    private String  param;

    /**
     * 导出名称
     */
    private String  name;

    /**
     * 导出类型 1付费会员卡
     */
    private Integer type;

    private String  applyUser;

    private String  extend;
}
