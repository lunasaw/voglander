package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.guard;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.command.Gb28181CommandType;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181 服���端布防/撤防控制指令实现类。
 *
 * <h3>envelope 通道</h3>
 * <ul>
 * <li>{@code gb28181.Control.Guard}（白名单）：payload {@code {guardCmd: SetGuard|ResetGuard}}</li>
 * </ul>
 *
 * @author luna
 */
@Component
public class VoglanderServerGuardCommand extends AbstractVoglanderServerCommand {

    /** GB28181 布防指令值。 */
    public static final String SET_GUARD   = "SetGuard";
    /** GB28181 撤防指令值。 */
    public static final String RESET_GUARD = "ResetGuard";

    /**
     * 控制设备布防/撤防。
     *
     * @param deviceId 设备 ID
     * @param guardCmd 布防指令（{@link #SET_GUARD} / {@link #RESET_GUARD}）
     */
    public ResultDTO<Void> controlDeviceGuard(String deviceId, String guardCmd) {
        validateDeviceId(deviceId, "控制设备布防时设备ID不能为空");
        validateNotNull(guardCmd, "布防指令不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("guardCmd", guardCmd);
        return dispatchEnvelope(Gb28181CommandType.CONTROL_GUARD.type(), deviceId, payload);
    }

    public ResultDTO<Void> setGuard(String deviceId) {
        return controlDeviceGuard(deviceId, SET_GUARD);
    }

    public ResultDTO<Void> resetGuard(String deviceId) {
        return controlDeviceGuard(deviceId, RESET_GUARD);
    }
}
