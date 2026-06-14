package io.github.lunasaw.voglander.web.api.device.req;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 查询移动位置订阅请求（Web 入参）。
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceMobilePositionReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "设备ID不能为空")
    private String            deviceId;

    /**
     * 上报间隔（秒），可空走底层默认
     */
    private String            interval;
}
