package io.github.lunasaw.voglander.web.api.device.req;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备更新请求对象
 * 
 * @author luna
 * @date 2024/01/30
 */
@Data
public class DeviceUpdateReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（更新必需）
     */
    private Long              id;

    /**
     * 设备ID
     */
    private String            deviceId;

    /**
     * 自定义名称
     */
    private String            name;

    /**
     * IP地址
     */
    private String            ip;

    /**
     * 端口
     */
    private Integer           port;

    /**
     * 协议类型 {@link DeviceAgreementEnum}
     */
    private Integer           type;

    /**
     * 注册节点IP
     */
    private String            serverIp;

    /**
     * 状态 1在线 0离线
     */
    private Integer           status;

    /**
     * 扩展信息
     */
    private ExtendInfoReq     extendInfo;

    @Data
    public static class ExtendInfoReq {
        /**
         * 设备序列号
         */
        private String  serialNumber;

        /**
         * 传输协议 UDP/TCP
         */
        private String  transport;

        /**
         * 注册有效期
         */
        private Integer expires;

        /**
         * 密码
         */
        private String  password;

        /**
         * 数据流传输模式
         */
        private String  streamMode;

        /**
         * 编码
         */
        private String  charset;

        /**
         * 设备信息
         */
        private String  deviceInfo;
    }
}