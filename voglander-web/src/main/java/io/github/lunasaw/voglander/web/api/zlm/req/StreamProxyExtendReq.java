package io.github.lunasaw.voglander.web.api.zlm.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author luna
 * @date 2025/8/14
 */
@Data
public class StreamProxyExtendReq {

    // ================================
    // ZLM 核心配置参数
    // ================================

    @Schema(description = "虚拟主机", example = "__defaultVhost__")
    private String  vhost;

    @Schema(description = "拉流重试次数，默认-1无限重试", example = "-1")
    private Integer retryCount;

    @Schema(description = "RTSP拉流方式：0=TCP，1=UDP，2=组播", example = "0")
    private Integer rtpType;

    @Schema(description = "拉流超时时间(秒)", example = "10")
    private Integer timeoutSec;

    // ================================
    // 协议转换启用开关
    // ================================

    @Schema(description = "是否转换成HLS-MPEGTS协议", example = "true")
    private Boolean enableHls;

    @Schema(description = "是否转换成HLS-FMP4协议", example = "false")
    private Boolean enableHlsFmp4;

    @Schema(description = "是否允许MP4录制", example = "false")
    private Boolean enableMp4;

    @Schema(description = "是否转RTSP协议", example = "true")
    private Boolean enableRtsp;

    @Schema(description = "是否转RTMP/FLV协议", example = "true")
    private Boolean enableRtmp;

    @Schema(description = "是否转HTTP-TS/WS-TS协议", example = "false")
    private Boolean enableTs;

    @Schema(description = "是否转HTTP-FMP4/WS-FMP4协议", example = "false")
    private Boolean enableFmp4;

    // ================================
    // 按需生成控制参数
    // ================================

    @Schema(description = "HLS是否按需生成(有人观看才生成)", example = "false")
    private Boolean hlsDemand;

    @Schema(description = "RTSP是否按需生成(有人观看才生成)", example = "false")
    private Boolean rtspDemand;

    @Schema(description = "RTMP是否按需生成(有人观看才生成)", example = "false")
    private Boolean rtmpDemand;

    @Schema(description = "TS是否按需生成(有人观看才生成)", example = "false")
    private Boolean tsDemand;

    @Schema(description = "FMP4是否按需生成(有人观看才生成)", example = "false")
    private Boolean fmp4Demand;

    // ================================
    // 音频处理参数
    // ================================

    @Schema(description = "转协议时是否开启音频", example = "true")
    private Boolean enableAudio;

    @Schema(description = "无音频时是否添加静音AAC音频", example = "false")
    private Boolean addMuteAudio;

    // ================================
    // MP4录制配置参数
    // ================================

    @Schema(description = "MP4录制文件保存根目录，置空使用默认")
    private String  mp4SavePath;

    @Schema(description = "MP4录制切片大小(秒)", example = "300")
    private Integer mp4MaxSecond;

    @Schema(description = "MP4录制是否当作观看者参与播放人数计数", example = "false")
    private Boolean mp4AsPlayer;

    // ================================
    // HLS录制配置参数
    // ================================

    @Schema(description = "HLS文件保存根目录，置空使用默认")
    private String  hlsSavePath;

    // ================================
    // 高级配置参数
    // ================================

    @Schema(description = "时间戳覆盖模式：0=绝对时间戳，1=系统时间戳，2=相对时间戳", example = "0")
    private Integer modifyStamp;

    @Schema(description = "无人观看是否自动关闭流(不触发无人观看hook)", example = "false")
    private Boolean autoClose;
}
