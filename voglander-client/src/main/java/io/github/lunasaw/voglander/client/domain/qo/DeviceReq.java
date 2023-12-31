package io.github.lunasaw.voglander.client.domain.qo;

import lombok.Data;

import java.util.Date;

/**
 * @author luna
 * @date 2023/12/30
 */
@Data
public class DeviceReq {

    /**
     * 设备Id
     */
    private String deviceId;

    /**
     * 注册时间
     */
    private Date registerTime;

    /**
     * 注册过期时间
     */
    private Integer expire;

    /**
     * 注册协议
     */
    private String transport;

    /**
     * 设备注册地址当前IP
     */
    private String localIp;

    /**
     * nat转换后看到的IP
     */
    private String remoteIp;

    /**
     * 经过rpotocol转换后的端口
     */
    private Integer remotePort;

    /**
     * 协议类型 {@link io.github.lunasaw.gb28181.common.entity.enums.DeviceAgreementEnum}
     */
    private Integer type;

}
