package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 协议验证台模拟推流配置。
 * <p>
 * 前缀 {@code voglander.protocol-lab.push}，仅在 lab profile 生效（与 {@link LabMediaPushService}
 * 同走 {@code voglander.protocol-lab.enabled=true} 条件化），生产 profile 不注册。
 * </p>
 *
 * @author luna
 */
@Data
@Component
@ConfigurationProperties(prefix = "voglander.protocol-lab.push")
public class LabPushProperties {

    /**
     * ffmpeg 可执行文件绝对路径。默认取 PATH 中的 {@code ffmpeg}。
     */
    private String  ffmpegPath = "ffmpeg";

    /**
     * 待推视频文件绝对路径。
     */
    private String  mediaFile;

    /**
     * 收到 INVITE 是否自动起 ffmpeg。
     * <p>
     * true=闭环无时序压力（平台 8s future 内自动喂流）；false=须前端手动点「模拟推流」且在 8s 窗内。
     * </p>
     */
    private boolean auto       = true;

    /**
     * 是否循环推流（{@code -stream_loop -1}），便于长时间联调。
     */
    private boolean loop       = true;

    /**
     * true=转码 libx264（兼容任意输入文件）；false=copy（输入须已是 H264/H265）。
     */
    private boolean transcode  = true;

    /**
     * 设备侧应答 SDP 的媒体 IP（{@code c=} 行）。留空回退 {@code sip.client.domain}。
     */
    private String  mediaIp;

    /**
     * 允许推流的文件白名单根目录（路径穿越防护）。留空=仅校验文件存在/可读。
     */
    private String  allowedRoot;
}
