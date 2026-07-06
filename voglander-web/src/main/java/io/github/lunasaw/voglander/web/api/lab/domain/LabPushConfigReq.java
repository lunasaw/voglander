package io.github.lunasaw.voglander.web.api.lab.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模拟推流运行时配置更新请求（不持久化）。
 */
@Data
@Schema(description = "模拟推流运行时配置")
public class LabPushConfigReq {

    @Schema(description = "true=ZLM中继模式；false=ffmpeg直推RTP")
    private Boolean zlmMode;
}
