package io.github.lunasaw.voglander.web.api.common.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用枚举VO模型
 * 
 * @author luna
 * @date 2024/01/30
 */
@Data
public class EnumVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 枚举值
     */
    private Integer           value;

    /**
     * 枚举编码
     */
    private String            code;

    /**
     * 枚举描述
     */
    private String            desc;

    public EnumVO() {}

    public EnumVO(Integer value, String code, String desc) {
        this.value = value;
        this.code = code;
        this.desc = desc;
    }

    public static EnumVO of(Integer value, String code, String desc) {
        return new EnumVO(value, code, desc);
    }
}