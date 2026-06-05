package io.github.lunasaw.voglander.web.api.ptz;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.web.api.ptz.assembler.PtzWebAssembler;
import io.github.lunasaw.voglander.web.api.ptz.controller.PtzController;
import io.github.lunasaw.voglander.web.api.ptz.domain.PresetReq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D6 预置位「档 B 保底」红线：{@code /ptz/preset} 直接返回明确「预置位暂不支持」，
 * 不再经 {@code ptzControl}/{@code PTZControlEnum.valueOf}（杜绝构造无效 {@code PRESET_*} 串 + 假成功 500 误导前端）。
 *
 * @author luna
 */
@DisplayName("D6 — PTZ 预置位保底返回不支持")
@ExtendWith(MockitoExtension.class)
class PtzControllerPresetTest {

    @Mock
    private DeviceCommandService deviceCommandService;
    @Mock
    private PtzWebAssembler      ptzWebAssembler;

    @InjectMocks
    private PtzController        controller;

    @Test
    @DisplayName("preset 直接返回不支持，且不触达 deviceCommandService/assembler")
    void preset_returnsUnsupported_withoutDispatch() {
        PresetReq req = new PresetReq();
        req.setDeviceId("dev1");
        req.setChannelId("ch1");
        req.setAction("SET");
        req.setPresetId(1);

        AjaxResult<Boolean> result = controller.preset(req);

        assertFalse(result.isSuccess(), "预置位应返回失败（不支持），不可假成功");
        // 不应再经协议下发路径（杜绝无效 PRESET_* 串 + valueOf 500）
        verifyNoInteractions(deviceCommandService);
        verifyNoInteractions(ptzWebAssembler);
    }
}
