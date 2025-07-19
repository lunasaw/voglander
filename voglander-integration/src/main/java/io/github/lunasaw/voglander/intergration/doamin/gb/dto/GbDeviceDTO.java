package io.github.lunasaw.voglander.intergration.doamin.gb.dto;

import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author luna
 * @date 2024/1/2
 */
@Data
public class GbDeviceDTO {

    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
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
    private LocalDateTime registerTime;
    //心跳时间
    private LocalDateTime keepaliveTime;
    //注册节点
    private String serverIp;
    /**
     * 协议类型 {@link DeviceAgreementEnum}
     */
    private Integer type;
    //扩展字段
    private String extend;

    private GbExtendInfo gbExtendInfo;


    @Data
    public static class GbExtendInfo {
        /**
         * 设备序列号
         */
        private String serialNumber;

        /**
         * 传输协议
         * UDP/TCP
         */
        private String transport;

        /**
         * 注册有效期
         */
        private int expires;

        /**
         * 密码
         */
        private String password;

        /**
         * 数据流传输模式
         * UDP:udp传输
         * TCP-ACTIVE：tcp主动模式
         * TCP-PASSIVE：tcp被动模式
         */
        private String streamMode;

        /**
         * 编码
         */
        private String charset;

        /**
         * 设备信息
         */
        private DeviceInfo deviceInfo;
    }
}
