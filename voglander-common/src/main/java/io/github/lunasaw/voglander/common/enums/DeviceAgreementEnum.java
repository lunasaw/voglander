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
     * GB28181国标协议
     */
    GB28181(1, "GB28181国标协议"),

    /**
     * 摄像头设备
     */
    CAMERA(2, "摄像头设备"),

    /**
     * NVR网络视频录像机
     */
    NVR(3, "NVR网络视频录像机"),

    /**
     * DVR数字视频录像机
     */
    DVR(4, "DVR数字视频录像机")

    ;

    private final Integer type;
    private final String desc;

    DeviceAgreementEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
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
     * 根据字符串名称获取对应的类型值
     */
    public static Integer getTypeByName(String name) {
        if (name == null) {
            return null;
        }
        switch (name.toLowerCase()) {
            case "gb28181":
                return GB28181.getType();
            case "camera":
                return CAMERA.getType();
            case "nvr":
                return NVR.getType();
            case "dvr":
                return DVR.getType();
            default:
                return null;
        }
    }

}
