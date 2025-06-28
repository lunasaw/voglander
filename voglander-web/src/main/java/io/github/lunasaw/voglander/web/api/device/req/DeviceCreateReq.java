package io.github.lunasaw.voglander.web.api.device.req;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备创建请求对象
 * @author luna
 * @date 2024/01/30
 */
@Data
public class DeviceCreateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 自定义名称
     */
    private String name;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 协议类型 {@link DeviceAgreementEnum}
     */
    private Integer type;

    /**
     * 设备种类 {@link io.github.lunasaw.voglander.common.enums.DeviceSubTypeEnum}
     */
    private Integer           subType;

    /**
     * 设备协议 {@link io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum}
     */
    private Integer           protocol;

    /**
     * 注册节点IP
     */
    private String serverIp;

    /**
     * 扩展信息
     */
    private ExtendInfoReq extendInfo;

    @Data
    public static class ExtendInfoReq {
        /**
         * 设备序列号
         */
        private String serialNumber;

        /**
         * 传输协议 UDP/TCP
         */
        private String transport;

        /**
         * 注册有效期
         */
        private Integer expires;

        /**
         * 密码
         */
        private String password;

        /**
         * 数据流传输模式
         */
        private String streamMode;

        /**
         * 编码
         */
        private String charset;

        /**
         * 设备信息
         */
        private String deviceInfo;
    }
}