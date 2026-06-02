package io.github.lunasaw.voglander.manager.domaon.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 媒体会话数据传输对象
 *
 * @author luna
 * @since 2025-05-29
 */
@Data
public class MediaSessionDTO {

    /**
     * 主键ID
     */
    private Long          id;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * SIP Call-ID，会话唯一标识（业务主键）
     */
    private String        callId;

    /**
     * 设备 GB28181 编码
     */
    private String        deviceId;

    /**
     * 通道 GB28181 编码
     */
    private String        channelId;

    /**
     * 媒体流 SSRC
     */
    private String        ssrc;

    /**
     * 媒体流标识（app/stream 或 ZLM stream id）
     */
    private String        stream;

    /**
     * 会话状态 1活跃(ACTIVE) 0已关闭(CLOSED) 2邀请中(INVITING) 3失败(FAILED)
     */
    private Integer       status;

    /**
     * 会话类型 PLAY(实时点播) PLAYBACK(回放) DOWNLOAD(下载) TALK(对讲)
     */
    private String        sessionType;

    /**
     * 扩展字段（JSON）
     */
    private String        extend;
}
