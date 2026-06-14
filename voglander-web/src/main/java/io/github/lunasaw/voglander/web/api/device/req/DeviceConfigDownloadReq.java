package io.github.lunasaw.voglander.web.api.device.req;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 下载设备配置请求（Web 入参）。
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceConfigDownloadReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "设备ID不能为空")
    private String            deviceId;

    /**
     * 配置类型 BASIC/VIDEO/AUDIO
     */
    @NotBlank(message = "配置类型不能为空")
    private String            configType;
}
