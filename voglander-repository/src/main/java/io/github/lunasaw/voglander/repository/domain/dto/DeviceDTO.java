package io.github.lunasaw.voglander.repository.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author chenzhangyue
 * @since 2023-12-28 11:11:54
 */
@SuppressWarnings("serial")
@Data
public class DeviceDTO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    private Long id;
    private Date createTime;
    private Date updateTime;
    //设备ID
    private String deviceId;
    //状态 1在线 0离线
    private Integer status;
    //自定义名称
    private String name;
    //IP
    private String ip;
    //端口
    private Integer port;
    //注册时间
    private Date registerTime;
    //心跳时间
    private Date keepaliveTime;
    //注册节点
    private String serverIp;
    //扩展字段
    private String extend;

    private ExtendInfo extendInfo;

    @Data
    public static class ExtendInfo {

        /**
         * 生产厂商
         */
        private String manufacturer;

        /**
         * 型号
         */

        private String model;

        /**
         * 固件版本
         */

        private String firmware;

        /**
         * 传输协议
         * UDP/TCP
         */
        private String transport;

        /**
         * 数据流传输模式
         * UDP:udp传输
         * TCP-ACTIVE：tcp主动模式
         * TCP-PASSIVE：tcp被动模式
         */
        private String streamMode;


        /**
         * 通道个数
         */
        private int channelCount;

        /**
         * 注册有效期
         */
        private int expires;

    }

}
