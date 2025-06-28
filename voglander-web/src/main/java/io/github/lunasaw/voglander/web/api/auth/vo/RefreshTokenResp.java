package io.github.lunasaw.voglander.web.api.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 刷新token响应VO
 *
 * @author luna
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据
     */
    private String            data;

    /**
     * 状态码
     */
    private Integer           status;
}