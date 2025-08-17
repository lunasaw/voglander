package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 推流代理查询请求
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "推流代理查询请求")
public class PushProxyQueryReq {

    @Schema(description = "推流代理ID", example = "1")
    private Long    id;

    @Schema(description = "应用名称", example = "live")
    private String  app;

    @Schema(description = "流名称", example = "test")
    private String  stream;

    @Schema(description = "推流目标地址", example = "rtmp://push.example.com/live/test")
    private String  dstUrl;

    @Schema(description = "推流协议", example = "rtmp")
    private String  schema;

    @Schema(description = "代理状态", example = "1", allowableValues = {"0", "1"})
    private Integer status;

    @Schema(description = "在线状态", example = "1", allowableValues = {"0", "1"})
    private Integer onlineStatus;

    @Schema(description = "代理密钥", example = "push_proxy_key")
    private String  proxyKey;

    @Schema(description = "节点ID", example = "zlm-node-1")
    private String  serverId;

    @Schema(description = "是否启用", example = "1", allowableValues = {"0", "1"})
    private Integer enabled;

    @Schema(description = "代理描述", example = "测试推流代理")
    private String  description;
}