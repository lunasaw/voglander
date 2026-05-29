package io.github.lunasaw.voglander.common.constant.media;

/**
 * 媒体会话相关常量
 *
 * @author luna
 * @since 2025-05-29
 */
public interface MediaSessionConstant {

    /**
     * 会话状态
     */
    interface Status {
        /** 已关闭 */
        int CLOSED = 0;
        /** 活跃（INVITE 200 OK 已建立） */
        int ACTIVE = 1;
        /** 邀请中（已发起 INVITE，等待应答） */
        int INVITING = 2;
        /** 失败（INVITE 失败或异常） */
        int FAILED = 3;
    }

    /**
     * 会话类型
     */
    interface Type {
        /** 实时点播 */
        String PLAY = "PLAY";
        /** 历史回放 */
        String PLAYBACK = "PLAYBACK";
        /** 文件下载 */
        String DOWNLOAD = "DOWNLOAD";
        /** 语音对讲 */
        String TALK = "TALK";
    }
}
