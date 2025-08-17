package io.github.lunasaw.voglander.web.api.zlm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 推流代理分页列表响应
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Schema(description = "推流代理分页列表响应")
public class PushProxyListResp {

    @Schema(description = "总记录数", example = "100")
    private Long              total;

    @Schema(description = "推流代理列表")
    private List<PushProxyVO> items;
}