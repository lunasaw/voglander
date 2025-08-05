package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 拉流代理管理表实体类
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_stream_proxy")
public class StreamProxyDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long              id;

    /**
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 修改时间
     */
    private LocalDateTime     updateTime;

    /**
     * 应用名称
     */
    private String            app;

    /**
     * 流ID
     */
    private String            stream;

    /**
     * 拉流地址
     */
    private String            url;

    /**
     * 代理状态 1启用 0禁用
     */
    private Integer           status;

    /**
     * 流在线状态 1在线 0离线
     */
    private Integer           onlineStatus;

    /**
     * ZLM返回的代理key
     */
    private String            proxyKey;

    /**
     * 代理描述
     */
    private String            description;

    /**
     * 是否启用 1启用 0禁用
     */
    private Boolean           enabled;

    /**
     * 扩展字段
     */
    private String            extend;
}