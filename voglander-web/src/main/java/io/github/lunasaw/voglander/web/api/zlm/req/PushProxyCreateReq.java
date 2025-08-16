package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 推流代理创建请求
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "推流代理创建请求")
public class PushProxyCreateReq {

    @NotBlank(message = "应用名称不能为空")
    @Schema(description = "应用名称", example = "live", requiredMode = Schema.RequiredMode.REQUIRED)
    private String             app;

    @NotBlank(message = "流名称不能为空")
    @Schema(description = "流名称", example = "test", requiredMode = Schema.RequiredMode.REQUIRED)
    private String             stream;

    @NotBlank(message = "推流目标地址不能为空")
    @Schema(description = "推流目标地址", example = "rtmp://push.example.com/live/test", requiredMode = Schema.RequiredMode.REQUIRED)
    private String             dstUrl;

    @Schema(description = "推流协议", example = "rtmp", defaultValue = "rtmp")
    private String             schema;

    @Schema(description = "代理状态", example = "1", allowableValues = {"0", "1"}, defaultValue = "1")
    private Integer            status;

    @NotBlank(message = "节点ID不能为空")
    @Schema(description = "节点ID", example = "zlm-node-1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String             serverId;

    @Schema(description = "代理描述", example = "测试推流代理")
    private String             description;

    @Schema(description = "ZLM推流扩展参数")
    private PushProxyExtendReq pushProxyExtendReq;
}