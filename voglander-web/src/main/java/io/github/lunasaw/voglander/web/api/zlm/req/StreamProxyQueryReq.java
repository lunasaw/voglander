package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 拉流代理查询请求
 * <p>
 * 用于灵活查询条件，支持多种查询组合
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "拉流代理查询请求")
public class StreamProxyQueryReq {

    @Schema(description = "代理ID")
    private Long    id;

    @Schema(description = "应用名称", example = "live")
    private String  app;

    @Schema(description = "流ID", example = "test")
    private String  stream;

    @Schema(description = "代理Key")
    private String  proxyKey;

    @Schema(description = "拉流地址")
    private String  url;

    @Schema(description = "代理描述")
    private String  description;

    @Schema(description = "状态 1启用 0禁用")
    private Integer status;

    @Schema(description = "在线状态 1在线 0离线")
    private Integer onlineStatus;

    @Schema(description = "服务器ID")
    private String  serverId;
}