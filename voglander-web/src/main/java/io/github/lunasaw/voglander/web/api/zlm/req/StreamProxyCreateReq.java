package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 拉流代理创建请求
 * <p>
 * 包含 /zlm/api/proxy/add 的所有参数，入参平铺设计，支持扩展模型自动映射
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "拉流代理创建请求 - 支持完整ZLM参数")
public class StreamProxyCreateReq {

    // ================================
    // 核心必填参数
    // ================================

    @NotBlank(message = "应用名称不能为空")
    @Schema(description = "应用名称", example = "live")
    private String  app;

    @NotBlank(message = "流ID不能为空")
    @Schema(description = "流ID", example = "test")
    private String  stream;

    @NotBlank(message = "拉流地址不能为空")
    @Schema(description = "拉流地址", example = "rtmp://live.hkstv.hk.lxdns.com/live/hks2")
    private String  url;

    // ================================
    // 业务管理参数
    // ================================

    @Schema(description = "代理描述")
    private String  description;

    @Schema(description = "是否启用", example = "1")
    private Integer status;

    @Schema(description = "节点ID，指定创建代理的ZLM节点", example = "zlm-node-1")
    private String  serverId;

    // ================================
    // 扩展参数
    // ================================
    @Schema(description = "ZLM扩展参数对象")
    private StreamProxyExtendReq streamProxyExtendReq;
}