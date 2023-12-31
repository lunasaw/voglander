package io.github.lunasaw.gb28181.common.entity.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * @author luna
 * @date 2023/10/13
 */
@Getter
public enum DeviceAgreementEnum {

    /**
     *
     */
    GB28181(1, "GB28181国标协议"),


    ;

    private final Integer type;
    private final String desc;

    DeviceAgreementEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static boolean isValid(Integer sort) {
        return Objects.equals(GB28181.getType(), sort);
    }

}
