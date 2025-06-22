package io.github.lunasaw.voglander.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 设备协议枚举
 * 
 * @author luna
 * @date 2024/01/30
 */
@Getter
public enum DeviceProtocolEnum {

    /**
     * GB28181国标协议
     */
    GB28181(1, "GB28181", "GB28181国标协议"),

    /**
     * ONVIF协议
     */
    ONVIF(2, "ONVIF", "ONVIF协议"),

    /**
     * RTSP协议
     */
    RTSP(3, "RTSP", "RTSP协议"),

    /**
     * HTTP协议
     */
    HTTP(4, "HTTP", "HTTP协议"),

    /**
     * RTMP协议
     */
    RTMP(5, "RTMP", "RTMP协议"),

    /**
     * 私有协议
     */
    PRIVATE(6, "PRIVATE", "私有协议"),

    ;

    private final Integer type;
    private final String  code;
    private final String  desc;

    DeviceProtocolEnum(Integer type, String code, String desc) {
        this.type = type;
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据类型获取枚举
     */
    public static DeviceProtocolEnum getByType(Integer type) {
        return Arrays.stream(DeviceProtocolEnum.values())
            .filter(e -> Objects.equals(e.getType(), type))
            .findFirst()
            .orElse(null);
    }

    /**
     * 根据编码获取枚举
     */
    public static DeviceProtocolEnum getByCode(String code) {
        return Arrays.stream(DeviceProtocolEnum.values())
            .filter(e -> Objects.equals(e.getCode(), code))
            .findFirst()
            .orElse(null);
    }

    /**
     * 验证类型是否有效
     */
    public static boolean isValidType(Integer type) {
        return Arrays.stream(DeviceProtocolEnum.values())
            .anyMatch(e -> Objects.equals(e.getType(), type));
    }

    /**
     * 验证编码是否有效
     */
    public static boolean isValidCode(String code) {
        return Arrays.stream(DeviceProtocolEnum.values())
            .anyMatch(e -> Objects.equals(e.getCode(), code));
    }

}