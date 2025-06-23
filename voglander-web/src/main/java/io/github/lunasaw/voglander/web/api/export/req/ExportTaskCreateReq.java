package io.github.lunasaw.voglander.web.api.export.req;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导出任务创建请求对象
 *
 * @author chenzhangyue
 * @since 2024-01-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportTaskCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务唯一Id
     */
    private Long bizId;

    /**
     * 导出的记录总数
     */
    private Long memberCnt;

    /**
     * 文件格式
     */
    private String format;

    /**
     * 申请时间
     */
    private Date applyTime;

    /**
     * 搜索条件序列化
     */
    private String param;

    /**
     * 导出名称
     */
    private String name;

    /**
     * 导出类型
     */
    private Integer type;

    /**
     * 申请用户
     */
    private String applyUser;

    /**
     * 扩展字段
     */
    private String extend;
}