package io.github.lunasaw.voglander.service.login;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 2a：DeviceRegisterServiceImpl 心跳/离线改调定向更新的纯单元测试。
 * <p>
 * 校验心跳走 {@link DeviceManager#patchLiveness}、离线走 {@link DeviceManager#patchOfflineTerminal}，
 * 不再走"读整行 + saveOrUpdate"全行写（修 P3 写放大）；并保留"设备不存在"语义。
 * </p>
 *
 * @author luna
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class DeviceRegisterServiceLivenessTest {

    private static final String DEVICE_ID = "34020000001320000001";

    @Mock
    private DeviceManager             deviceManager;

    @InjectMocks
    private DeviceRegisterServiceImpl deviceRegisterService;

    @Test
    public void testKeepaliveUsesPatchLiveness() {
        // Given：设备存在
        DeviceDTO existing = new DeviceDTO();
        existing.setDeviceId(DEVICE_ID);
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(existing);

        // When
        AjaxResult<Boolean> result = deviceRegisterService.keepalive(DEVICE_ID);

        // Then：走定向单调更新，不走 saveOrUpdate 全行写
        verify(deviceManager, times(1)).patchLiveness(eq(DEVICE_ID), eq(DeviceConstant.Status.ONLINE), any(LocalDateTime.class));
        verify(deviceManager, never()).saveOrUpdate(any(DeviceDTO.class));
        assertTrue((Boolean) result.getData(), "心跳成功应返回 true");
        log.info("心跳改调 patchLiveness 校验通过");
    }

    @Test
    public void testKeepaliveOnNonExistentDeviceReturnsFalse() {
        // Given：设备不存在
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(null);

        // When
        AjaxResult<Boolean> result = deviceRegisterService.keepalive(DEVICE_ID);

        // Then：保留"设备不存在"语义，不触发任何写
        assertFalse((Boolean) result.getData(), "设备不存在应返回 false");
        verify(deviceManager, never()).patchLiveness(any(), any(), any());
        verify(deviceManager, never()).saveOrUpdate(any(DeviceDTO.class));
        log.info("不存在设备的心跳保留原语义校验通过");
    }

    @Test
    public void testOfflineUsesPatchOfflineTerminal() {
        // Given：设备存在
        DeviceDTO existing = new DeviceDTO();
        existing.setDeviceId(DEVICE_ID);
        when(deviceManager.getDtoByDeviceId(DEVICE_ID)).thenReturn(existing);

        // When
        deviceRegisterService.offline(DEVICE_ID);

        // Then：走终态定向写，不走 updateStatus 全行写
        verify(deviceManager, times(1)).patchOfflineTerminal(DEVICE_ID);
        verify(deviceManager, never()).updateStatus(any(), eq(DeviceConstant.Status.OFFLINE));
        log.info("离线改调 patchOfflineTerminal 校验通过");
    }
}
