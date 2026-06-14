package io.github.lunasaw.voglander.web.api.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.service.subscription.DeviceSubscriptionService;
import io.github.lunasaw.voglander.web.api.device.req.SubscriptionToggleReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备订阅（GB28181-2022 §9.11：目录 / 位置 / 告警）开关接口。
 *
 * @author luna
 */
@Slf4j
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/device/subscription")
@Tag(name = "设备订阅", description = "GB28181 目录/位置/告警订阅开关")
public class DeviceSubscriptionController {

    @Autowired
    private DeviceSubscriptionService subscriptionService;

    @PutMapping("/toggle")
    @Operation(summary = "开关订阅", description = "开启/关闭设备的目录/位置/告警订阅；开关即下发/撤销 SUBSCRIBE")
    public AjaxResult<Boolean> toggle(@Valid @RequestBody SubscriptionToggleReq req) {
        SubscriptionConstant.Type type;
        try {
            type = SubscriptionConstant.Type.valueOf(req.getType());
        } catch (IllegalArgumentException e) {
            return AjaxResult.error("非法订阅类型: " + req.getType());
        }
        boolean ok = Boolean.TRUE.equals(req.getEnabled())
            ? subscriptionService.enable(req.getDeviceId(), type)
            : subscriptionService.disable(req.getDeviceId(), type);
        return AjaxResult.success(ok);
    }
}
