package io.github.lunasaw.voglander.manager.domaon.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 拉流代理数据传输对象
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
public class StreamProxyDTO {

    /**
     * 主键ID
     */
    private Long          id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 应用名称
     */
    private String        app;

    /**
     * 流ID
     */
    private String        stream;

    /**
     * 拉流地址
     */
    private String        url;

    /**
     * 代理状态 1启用 0禁用
     */
    private Integer       status;

    /**
     * 流在线状态 1在线 0离线
     */
    private Integer       onlineStatus;

    /**
     * ZLM返回的代理key
     */
    private String        proxyKey;

    /**
     * 代理描述
     */
    private String        description;

    /**
     * 是否启用 1启用 0禁用
     */
    private Boolean       enabled;

    /**
     * 扩展字段
     */
    private String        extend;
}