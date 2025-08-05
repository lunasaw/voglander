package io.github.lunasaw.voglander.web.api.zlm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 拉流代理列表响应
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "拉流代理列表响应")
public class StreamProxyListResp {

    @Schema(description = "总记录数")
    private Long                total;

    @Schema(description = "拉流代理列表")
    private List<StreamProxyVO> items;
}