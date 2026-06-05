package io.github.lunasaw.voglander.service.live.dto;

import lombok.Data;

/**
 * 开始直播请求 DTO。
 *
 * @author luna
 */
@Data
public class LiveStartDTO {

    /**
     * 设备 GB28181 编码
     */
    private String deviceId;

    /**
     * 通道 GB28181 编码
     */
    private String channelId;

    /**
     * 播放协议偏好（FLV/HLS/...），前端自适应，仅作记录。
     */
    private String protocol   = "FLV";

    /**
     * 拉流模式 UDP / TCP_ACTIVE / TCP_PASSIVE。
     */
    private String streamMode = "UDP";
}
