package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 拉流代理创建请求
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "拉流代理创建请求")
public class StreamProxyCreateReq {

    @NotBlank(message = "应用名称不能为空")
    @Schema(description = "应用名称", example = "live")
    private String  app;

    @NotBlank(message = "流ID不能为空")
    @Schema(description = "流ID", example = "test")
    private String  stream;

    @NotBlank(message = "拉流地址不能为空")
    @Schema(description = "拉流地址", example = "rtmp://live.hkstv.hk.lxdns.com/live/hks2")
    private String  url;

    @Schema(description = "代理描述")
    private String  description;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "扩展字段")
    private String  extend;
}