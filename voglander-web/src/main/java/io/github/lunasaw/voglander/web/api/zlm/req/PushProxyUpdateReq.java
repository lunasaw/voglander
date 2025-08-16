package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 推流代理更新请求
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "推流代理更新请求")
public class PushProxyUpdateReq {

    @NotNull(message = "推流代理ID不能为空")
    @Schema(description = "推流代理ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long               id;

    @Schema(description = "应用名称", example = "live")
    private String             app;

    @Schema(description = "流名称", example = "test")
    private String             stream;

    @Schema(description = "推流目标地址", example = "rtmp://push.example.com/live/test")
    private String             dstUrl;

    @Schema(description = "推流协议", example = "rtmp")
    private String             schema;

    @Schema(description = "代理状态", example = "1", allowableValues = {"0", "1"})
    private Integer            status;

    @Schema(description = "节点ID", example = "zlm-node-1")
    private String             serverId;

    @Schema(description = "代理描述", example = "测试推流代理")
    private String             description;

    @Schema(description = "ZLM推流扩展参数")
    private PushProxyExtendReq pushProxyExtendReq;
}