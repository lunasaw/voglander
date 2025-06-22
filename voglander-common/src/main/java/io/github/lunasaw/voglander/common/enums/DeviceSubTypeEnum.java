package io.github.lunasaw.voglander.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 设备类型枚举
 * 
 * @author luna
 * @date 2024/01/30
 */
@Getter
public enum DeviceSubTypeEnum {

    /**
     * 网络摄像头
     */
    IPC(1, "IPC", "网络摄像头"),

    /**
     * 网络硬盘录像机
     */
    NVR(2, "NVR", "网络硬盘录像机"),

    /**
     * 数字硬盘录像机
     */
    DVR(3, "DVR", "数字硬盘录像机"),

    /**
     * 编码器
     */
    ENCODER(4, "ENCODER", "编码器"),

    /**
     * 解码器
     */
    DECODER(5, "DECODER", "解码器"),

    /**
     * 平台网关
     */
    PLATFORM(6, "PLATFORM", "平台网关"),

    ;

    private final Integer type;
    private final String  code;
    private final String  desc;

    DeviceSubTypeEnum(Integer type, String code, String desc) {
        this.type = type;
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据类型获取枚举
     */
    public static DeviceSubTypeEnum getByType(Integer type) {
        return Arrays.stream(DeviceSubTypeEnum.values())
            .filter(e -> Objects.equals(e.getType(), type))
            .findFirst()
            .orElse(null);
    }

    /**
     * 根据编码获取枚举
     */
    public static DeviceSubTypeEnum getByCode(String code) {
        return Arrays.stream(DeviceSubTypeEnum.values())
            .filter(e -> Objects.equals(e.getCode(), code))
            .findFirst()
            .orElse(null);
    }

    /**
     * 验证类型是否有效
     */
    public static boolean isValidType(Integer type) {
        return Arrays.stream(DeviceSubTypeEnum.values())
            .anyMatch(e -> Objects.equals(e.getType(), type));
    }

    /**
     * 验证编码是否有效
     */
    public static boolean isValidCode(String code) {
        return Arrays.stream(DeviceSubTypeEnum.values())
            .anyMatch(e -> Objects.equals(e.getCode(), code));
    }

}