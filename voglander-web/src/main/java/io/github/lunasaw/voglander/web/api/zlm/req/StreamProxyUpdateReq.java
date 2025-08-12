package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 拉流代理更新请求
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "拉流代理更新请求")
public class StreamProxyUpdateReq {

    @NotNull(message = "代理ID不能为空")
    @Schema(description = "代理ID", required = true, example = "1")
    private Long    id;

    @Schema(description = "应用名称", example = "live")
    private String  app;

    @Schema(description = "流ID", example = "test")
    private String  stream;

    @Schema(description = "拉流地址", example = "rtmp://live.hkstv.hk.lxdns.com/live/hks2")
    private String  url;

    @Schema(description = "代理描述")
    private String  description;

    @Schema(description = "代理状态 1启用 0禁用", example = "1")
    private Integer status;

    @Schema(description = "节点ID，指定创建代理的ZLM节点", example = "zlm-node-1")
    private String  serverId;

    @Schema(description = "扩展字段")
    private String  extend;
}