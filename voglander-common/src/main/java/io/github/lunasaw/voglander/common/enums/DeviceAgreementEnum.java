package io.github.lunasaw.gb28181.common.entity.enums;

/**
 * @author luna
 * @date 2023/10/13
 */
public enum DeviceAgreementEnum {

    /**
     *
     */
    GB28181("1", "GB28181国标协议"),


    ;

    private final String type;
    private final String desc;

    DeviceAgreementEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static boolean isValid(String sort) {
        return GB28181.getType().equals(sort);
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}
