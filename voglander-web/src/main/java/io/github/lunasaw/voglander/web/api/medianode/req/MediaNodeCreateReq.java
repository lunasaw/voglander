package io.github.lunasaw.voglander.web.api.medianode.req;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.io.Serializable;

/**
 * 流媒体节点创建请求对象
 * @author luna
 * @date 2025/01/23
 */
@Data
public class MediaNodeCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    @NotBlank(message = "节点ID不能为空")
    private String serverId;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点地址
     */
    @NotBlank(message = "节点地址不能为空")
    private String host;

    /**
     * API密钥
     */
    private String secret;

    /**
     * 是否启用 true启用 false禁用
     */
    private Boolean enabled;

    /**
     * 是否启用Hook true启用 false禁用
     */
    private Boolean hookEnabled;

    /**
     * 节点权重
     */
    @Min(value = 1, message = "节点权重最小为1")
    @Max(value = 1000, message = "节点权重最大为1000")
    private Integer weight;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 扩展字段
     */
    private String extend;
}