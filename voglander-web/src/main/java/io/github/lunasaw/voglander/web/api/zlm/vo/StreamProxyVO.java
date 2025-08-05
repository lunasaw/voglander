package io.github.lunasaw.voglander.web.api.zlm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 拉流代理视图对象
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "拉流代理视图对象")
public class StreamProxyVO {

    @Schema(description = "主键ID")
    private Long    id;

    @Schema(description = "创建时间戳（毫秒）")
    private Long    createTime;

    @Schema(description = "修改时间戳（毫秒）")
    private Long    updateTime;

    @Schema(description = "应用名称")
    private String  app;

    @Schema(description = "流ID")
    private String  stream;

    @Schema(description = "拉流地址")
    private String  url;

    @Schema(description = "代理状态 1启用 0禁用")
    private Integer status;

    @Schema(description = "流在线状态 1在线 0离线")
    private Integer onlineStatus;

    @Schema(description = "ZLM返回的代理key")
    private String  proxyKey;

    @Schema(description = "代理描述")
    private String  description;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "扩展字段")
    private String  extend;

    /**
     * 创建时间转换为时间戳
     */
    public Long createTimeToEpochMilli() {
        return createTime;
    }

    /**
     * 修改时间转换为时间戳
     */
    public Long updateTimeToEpochMilli() {
        return updateTime;
    }
}