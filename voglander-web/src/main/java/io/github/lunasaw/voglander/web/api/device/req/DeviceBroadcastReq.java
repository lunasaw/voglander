package io.github.lunasaw.voglander.web.api.device.req;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 语音广播请求（Web 入参）。
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceBroadcastReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "设备ID不能为空")
    private String            deviceId;
}
