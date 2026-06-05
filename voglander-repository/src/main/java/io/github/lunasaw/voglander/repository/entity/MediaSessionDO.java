package io.github.lunasaw.voglander.repository.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 媒体会话表实体类
 * <p>
 * 管理 GB28181 INVITE 点播/回放会话状态，由 {@code VoglanderBusinessNotifier} 的
 * Session.* / Notify.MediaStatus 事件驱动写入。
 * </p>
 *
 * @author luna
 * @since 2025-05-29
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_media_session")
public class MediaSessionDO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long              id;

    /**
     * 创建时间
     */
    private LocalDateTime     createTime;

    /**
     * 修改时间
     */
    private LocalDateTime     updateTime;

    /**
     * SIP Call-ID，会话唯一标识（业务主键）
     */
    private String            callId;

    /**
     * 设备 GB28181 编码
     */
    private String            deviceId;

    /**
     * 通道 GB28181 编码
     */
    private String            channelId;

    /**
     * 媒体流 SSRC
     */
    private String            ssrc;

    /**
     * 媒体流标识（app/stream 或 ZLM stream id）
     */
    private String            stream;

    /**
     * 会话状态 1活跃(ACTIVE) 0已关闭(CLOSED) 2邀请中(INVITING) 3失败(FAILED)
     */
    private Integer           status;

    /**
     * 会话类型 PLAY(实时点播) PLAYBACK(回放) DOWNLOAD(下载) TALK(对讲)
     */
    private String            sessionType;

    /**
     * 扩展字段（JSON）
     */
    private String            extend;

    /**
     * 前端稳定主键，直播=gb_live_{deviceId}_{channelId}，回放=gb_back_{deviceId}_{channelId}_{ts}
     */
    private String            streamId;

    /**
     * 媒体节点 serverId（亲和路由）
     */
    private String            nodeServerId;

    /**
     * 观看者引用计数（DB 快照，Redis 为权威值）
     */
    private Integer           refCount;
}
