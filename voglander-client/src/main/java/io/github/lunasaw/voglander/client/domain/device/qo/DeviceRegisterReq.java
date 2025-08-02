package io.github.lunasaw.voglander.client.domain.device.qo;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author luna
 * @date 2023/12/30
 */
@Data
public class DeviceRegisterReq {

    /**
     * 设备Id
     */
    private String deviceId;

    /**
     * 注册时间
     */
    private LocalDateTime registerTime;

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
     * 协议类型 {@link DeviceAgreementEnum}
     */
    private Integer type;

    /**
     * 设备种类 {@link io.github.lunasaw.voglander.common.enums.DeviceSubTypeEnum}
     */
    private Integer subType;

    /**
     * 设备协议 {@link io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum}
     */
    private Integer protocol;

}
