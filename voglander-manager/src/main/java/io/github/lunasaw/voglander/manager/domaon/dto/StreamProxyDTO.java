package io.github.lunasaw.voglander.manager.domaon.dto;

import java.time.LocalDateTime;

import com.alibaba.fastjson2.annotation.JSONField;

import lombok.Data;

/**
 * 拉流代理数据传输对象
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
public class StreamProxyDTO {

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
     * 应用名称
     */
    private String        app;

    /**
     * 流ID
     */
    private String        stream;

    /**
     * 拉流地址
     */
    private String        url;

    /**
     * 代理状态 1启用 0禁用
     */
    private Integer       status;

    /**
     * 流在线状态 1在线 0离线
     */
    private Integer       onlineStatus;

    /**
     * ZLM返回的代理key
     */
    private String        proxyKey;

    /**
     * 节点ID，保存当前添加拉流代理的节点
     */
    private String        serverId;

    /**
     * 代理描述
     */
    private String        description;
    /**
     * 扩展字段
     */
    private String        extend;

    private ExtendObj     extendObj;

    @Data
    public static class ExtendObj {

        /**
         * 筛选协议，例如 rtsp或rtmp
         */
        private String  schema;

        /**
         * ZLM扩展参数对象
         */
        private String  vhost;

        /**
         * 拉流重试次数，默认为-1无限重试
         */
        private Integer retryCount;

        /**
         * rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播
         */
        private Integer rtpType;

        /**
         * 拉流超时时间，单位秒，float类型
         */
        private Integer timeoutSec;

        /**
         * 是否转换成hls-mpegts协议
         */
        private Boolean enableHls;

        /**
         * 是否转换成hls-fmp4协议
         */
        private Boolean enableHlsFmp4;

        /**
         * 是否允许mp4录制
         */
        private Boolean enableMp4;

        /**
         * 是否转rtsp协议
         */
        private Boolean enableRtsp;

        /**
         * 是否转rtmp/flv协议
         */
        private Boolean enableRtmp;

        /**
         * 是否转http-ts/ws-ts协议
         */
        private Boolean enableTs;

        /**
         * 是否转http-fmp4/ws-fmp4协议
         */
        private Boolean enableFmp4;

        /**
         * 该协议是否有人观看才生成
         */
        private Boolean hlsDemand;

        /**
         * 该协议是否有人观看才生成
         */
        private Boolean rtspDemand;

        /**
         * 该协议是否有人观看才生成
         */
        private Boolean rtmpDemand;

        /**
         * 该协议是否有人观看才生成
         */
        private Boolean tsDemand;

        /**
         * 该协议是否有人观看才生成
         */
        private Boolean fmp4Demand;

        /**
         * 转协议时是否开启音频
         */
        private Boolean enableAudio;

        /**
         * 转协议时，无音频是否添加静音aac音频
         */
        private Boolean addMuteAudio;

        /**
         * mp4录制文件保存根目录，置空使用默认
         */
        private String  mp4SavePath;

        /**
         * mp4录制切片大小，单位秒
         */
        private Integer mp4MaxSecond;

        /**
         * MP4录制是否当作观看者参与播放人数计数
         */
        private Boolean mp4AsPlayer;

        /**
         * hls文件保存保存根目录，置空使用默认
         */
        private String  hlsSavePath;

        /**
         * 该流是否开启时间戳覆盖(0:绝对时间戳/1:系统时间戳/2:相对时间戳)
         */
        private Integer modifyStamp;

        /**
         * 无人观看是否自动关闭流(不触发无人观看hook)
         */
        private Boolean autoClose;
    }
}