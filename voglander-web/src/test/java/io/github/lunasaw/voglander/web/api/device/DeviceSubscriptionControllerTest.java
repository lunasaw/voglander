package io.github.lunasaw.voglander.web.api.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.service.subscription.DeviceSubscriptionService;
import io.github.lunasaw.voglander.web.api.device.req.SubscriptionToggleReq;

/**
 * {@link DeviceSubscriptionController} 纯单元测试（Mockito）。
 * <p>
 * 覆盖：toggle enable/disable 分发、非法类型拒绝。
 * </p>
 *
 * @author luna
 */
@DisplayName("DeviceSubscriptionController toggle")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DeviceSubscriptionControllerTest {

    private static final String           DEVICE_ID = "34020000001320000001";

    @Mock
    private DeviceSubscriptionService     subscriptionService;

    @InjectMocks
    private DeviceSubscriptionController   controller;

    private SubscriptionToggleReq req(String type, boolean enabled) {
        SubscriptionToggleReq r = new SubscriptionToggleReq();
        r.setDeviceId(DEVICE_ID);
        r.setType(type);
        r.setEnabled(enabled);
        return r;
    }

    @Test
    @DisplayName("enabled=true → 调 enable")
    public void toggleOnCallsEnable() {
        when(subscriptionService.enable(DEVICE_ID, SubscriptionConstant.Type.CATALOG)).thenReturn(true);

        AjaxResult<Boolean> result = controller.toggle(req("CATALOG", true));

        assertEquals(0, result.get("code"));
        verify(subscriptionService).enable(DEVICE_ID, SubscriptionConstant.Type.CATALOG);
        verify(subscriptionService, never()).disable(DEVICE_ID, SubscriptionConstant.Type.CATALOG);
    }

    @Test
    @DisplayName("enabled=false → 调 disable")
    public void toggleOffCallsDisable() {
        when(subscriptionService.disable(DEVICE_ID, SubscriptionConstant.Type.ALARM)).thenReturn(true);

        controller.toggle(req("ALARM", false));

        verify(subscriptionService).disable(DEVICE_ID, SubscriptionConstant.Type.ALARM);
        verify(subscriptionService, never()).enable(DEVICE_ID, SubscriptionConstant.Type.ALARM);
    }

    @Test
    @DisplayName("非法订阅类型 → error，不分发")
    public void toggleInvalidTypeRejected() {
        AjaxResult<Boolean> result = controller.toggle(req("BOGUS", true));

        assertEquals(500, result.get("code"));
        verify(subscriptionService, never()).enable(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
    }
}
