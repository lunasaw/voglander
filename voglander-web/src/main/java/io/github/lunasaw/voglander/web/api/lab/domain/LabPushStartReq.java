package io.github.lunasaw.voglander.web.api.lab.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模拟推流启动请求。两字段均选填，留空则用 {@code voglander.protocol-lab.push.*} 配置默认值。
 *
 * @author luna
 */
@Data
@Schema(description = "协议验证台模拟推流启动请求")
public class LabPushStartReq {

    @Schema(description = "ffmpeg 可执行文件绝对路径，覆盖配置默认值")
    private String ffmpegPath;

    @Schema(description = "待推视频文件绝对路径，覆盖配置默认值")
    private String mediaFile;
}
