package io.github.lunasaw.voglander.common.enums;

import lombok.Getter;

import java.util.Objects;
import java.util.Arrays;

/**
 * @author luna
 * @date 2023/10/13
 */
@Getter
public enum DeviceAgreementEnum {

    /**
     * GB28181国标协议摄像头
     */
    GB28181_IPC(1, "GB28181国标协议摄像头", DeviceSubTypeEnum.IPC.getType(), DeviceProtocolEnum.GB28181.getType()),

    /**
     * GB28181 NVR设备
     */
    GB28181_NVR(2, "GB28181 NVR设备", DeviceSubTypeEnum.NVR.getType(), DeviceProtocolEnum.GB28181.getType()),

    /**
     * ONVIF协议摄像头
     */
    ONVIF_IPC(3, "ONVIF协议摄像头", DeviceSubTypeEnum.IPC.getType(), DeviceProtocolEnum.ONVIF.getType()),

    ;

    private final Integer type;
    private final String desc;
    private final Integer subType;
    private final Integer protocol;

    DeviceAgreementEnum(Integer type, String desc, Integer subType, Integer protocol) {
        this.type = type;
        this.desc = desc;
        this.subType = subType;
        this.protocol = protocol;
    }

    public static boolean isValid(Integer sort) {
        return Arrays.stream(DeviceAgreementEnum.values())
            .anyMatch(e -> Objects.equals(e.getType(), sort));
    }

    /**
     * 根据类型获取枚举
     */
    public static DeviceAgreementEnum getByType(Integer type) {
        return Arrays.stream(DeviceAgreementEnum.values())
            .filter(e -> Objects.equals(e.getType(), type))
            .findFirst()
            .orElse(null);
    }

    /**
     * 根据设备种类和协议获取枚举
     */
    public static DeviceAgreementEnum getBySubTypeAndProtocol(Integer subType, Integer protocol) {
        return Arrays.stream(DeviceAgreementEnum.values())
            .filter(e -> Objects.equals(e.getSubType(), subType) && Objects.equals(e.getProtocol(), protocol))
            .findFirst()
            .orElse(null);
    }

    /**
     * 根据设备种类和协议获取type值
     */
    public static Integer getTypeBySubTypeAndProtocol(Integer subType, Integer protocol) {
        DeviceAgreementEnum agreementEnum = getBySubTypeAndProtocol(subType, protocol);
        return agreementEnum != null ? agreementEnum.getType() : null;
    }

}
