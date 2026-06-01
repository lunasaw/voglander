package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端云台控制指令实现类
 *
 * <h3>sip-gateway 1.8.0 envelope 改造（2026-06-01）</h3>
 * <p>
 * 出站统一经 envelope 通道：构造 {@code GatewayCommand("gb28181.Control.Ptz", deviceId, payload, null)}
 * 由 {@code Gb28181WhitelistHandlers#ptz} 反查 schema 转 SIP 下发。
 * </p>
 * <p>
 * Payload schema：
 * <ul>
 * <li>枚举模式：{@code {cmd: PTZControlEnum 名称, speed: int}}</li>
 * <li>16 进制模式：{@code {hex: String}}</li>
 * </ul>
 * </p>
 *
 * @author luna
 * @since 2025/8/2
 * @version 2.0
 */
@Component
public class VoglanderServerPtzCommand extends AbstractVoglanderServerCommand {

    /**
     * envelope 命令类型（@CommandMapping 在 Gb28181WhitelistHandlers）。
     */
    private static final String TYPE_PTZ      = "gb28181.Control.Ptz";

    /**
     * 默认云台控制速度。
     */
    private static final int    DEFAULT_SPEED = 128;

    /**
     * 控制设备云台（使用自定义指令）：16 进制字节串模式。
     */
    public ResultDTO<Void> controlDevicePtz(String deviceId, String ptzCmd) {
        validateDeviceId(deviceId, "控制设备云台时设备ID不能为空");
        validateNotNull(ptzCmd, "云台控制���令不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("hex", ptzCmd);
        return dispatchEnvelope(TYPE_PTZ, deviceId, payload);
    }

    /**
     * 控制设备云台（使用枚举和速度）：枚举 + 速度模式。
     */
    public ResultDTO<Void> controlDevicePtz(String deviceId, PTZControlEnum control, Integer speed) {
        validateDeviceId(deviceId, "控制设备云台时设备ID不能为空");
        validateNotNull(control, "云台控制枚举不能为空");

        if (speed != null && (speed < 1 || speed > 255)) {
            throw new IllegalArgumentException("云台控制速度必须在1-255之间");
        }

        int finalSpeed = speed != null ? speed : DEFAULT_SPEED;
        Map<String, Object> payload = new HashMap<>();
        payload.put("cmd", control.name());
        payload.put("speed", finalSpeed);
        return dispatchEnvelope(TYPE_PTZ, deviceId, payload);
    }

    // ==================== 方向控制指令 ====================

    public ResultDTO<Void> moveUp(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.TILT_UP, speed);
    }

    public ResultDTO<Void> moveDown(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.TILT_DOWN, speed);
    }

    public ResultDTO<Void> moveLeft(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_LEFT, speed);
    }

    public ResultDTO<Void> moveRight(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_RIGHT, speed);
    }

    public ResultDTO<Void> moveUpLeft(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_LEFT_TILT_UP, speed);
    }

    public ResultDTO<Void> moveUpRight(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_RIGHT_TILT_UP, speed);
    }

    public ResultDTO<Void> moveDownLeft(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_LEFT_TILT_DOWN, speed);
    }

    public ResultDTO<Void> moveDownRight(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_RIGHT_TILT_DOWN, speed);
    }

    // ==================== 变焦控制指令 ====================

    public ResultDTO<Void> zoomIn(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.ZOOM_IN, speed);
    }

    public ResultDTO<Void> zoomOut(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.ZOOM_OUT, speed);
    }

    // ==================== 停止控制指令 ====================

    /**
     * 停止云台移动：cmd=STOP，speed=0。
     */
    public ResultDTO<Void> stopDevicePtz(String deviceId) {
        validateDeviceId(deviceId, "停止设备云台时设备ID不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("cmd", PTZControlEnum.STOP.name());
        payload.put("speed", 0);
        return dispatchEnvelope(TYPE_PTZ, deviceId, payload);
    }

    // ==================== 便捷方法（使用默认速度） ====================

    public ResultDTO<Void> controlWithDefaultSpeed(String deviceId, PTZControlEnum control) {
        return controlDevicePtz(deviceId, control, DEFAULT_SPEED);
    }

    public ResultDTO<Void> moveUp(String deviceId) {
        return moveUp(deviceId, DEFAULT_SPEED);
    }

    public ResultDTO<Void> moveDown(String deviceId) {
        return moveDown(deviceId, DEFAULT_SPEED);
    }

    public ResultDTO<Void> moveLeft(String deviceId) {
        return moveLeft(deviceId, DEFAULT_SPEED);
    }

    public ResultDTO<Void> moveRight(String deviceId) {
        return moveRight(deviceId, DEFAULT_SPEED);
    }

    public ResultDTO<Void> zoomIn(String deviceId) {
        return zoomIn(deviceId, DEFAULT_SPEED);
    }

    public ResultDTO<Void> zoomOut(String deviceId) {
        return zoomOut(deviceId, DEFAULT_SPEED);
    }
}
