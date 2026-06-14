package io.github.lunasaw.voglander.web.api.device.req;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设备订阅开关请求（GB28181-2022 §9.11）。
 *
 * @author luna
 */
@Data
public class SubscriptionToggleReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "设备国标编码")
    @NotBlank(message = "设备ID不能为空")
    private String  deviceId;

    @Schema(description = "订阅类型 CATALOG / MOBILE_POSITION / ALARM")
    @NotBlank(message = "订阅类型不能为空")
    private String  type;

    @Schema(description = "是否开启")
    @NotNull(message = "开关状态不能为空")
    private Boolean enabled;
}
