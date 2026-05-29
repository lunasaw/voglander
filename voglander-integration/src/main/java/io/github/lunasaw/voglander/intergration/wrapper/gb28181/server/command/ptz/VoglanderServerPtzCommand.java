package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端云台控制指令实现类
 * <p>
 * 提供云台控制相关的指令发送功能，包括云台移动、变焦、预置位等控制操作。
 * 继承AbstractVoglanderServerCommand获得统一的异常处理和日志记录能力。
 * </p>
 *
 * <h3>sip-gateway 1.8.0 适配</h3>
 * <p>
 * 1.8.0 移除了 {@code PtzCmdEnum}，方向/变焦控制统一使用
 * {@link PTZControlEnum} + speed（0-255）经
 * {@code ServerCommandSender.deviceControlPtzCmd(deviceId, PTZControlEnum, int)} 下发。
 * </p>
 *
 * <h3>支持的云台控制</h3>
 * <ul>
 * <li>方向控制 - 上、下、左、右、左上、右上、左下、右下</li>
 * <li>变焦控制 - 放大、缩小</li>
 * <li>自定义云台指令（16进制字节串）</li>
 * </ul>
 *
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Component
public class VoglanderServerPtzCommand extends AbstractVoglanderServerCommand {

    /**
     * 默认云台控制速度。
     */
    private static final int DEFAULT_SPEED = 128;

    /**
     * 控制设备云台（使用自定义指令）
     * <p>
     * 向设备发送自定义的云台控制指令（16进制字节串，8 字节）。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param ptzCmd 云台控制命令字符串（16进制格式）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或控制命令为空时抛出
     */
    public ResultDTO<Void> controlDevicePtz(String deviceId, String ptzCmd) {
        validateDeviceId(deviceId, "控制设备云台时设备ID不能为空");
        validateNotNull(ptzCmd, "云台控制命令不能为空");

        return executeCommand("controlDevicePtz", deviceId,
            () -> serverCommandSender.deviceControlPtzCmd(deviceId, ptzCmd),
            deviceId, ptzCmd);
    }

    /**
     * 控制设备云台（使用枚举和速度）
     * <p>
     * 使用预定义的云台控制枚举和速度参数控制设备云台。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param control 云台控制枚举
     * @param speed 控制速度（1-255），为空时使用默认速度
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> controlDevicePtz(String deviceId, PTZControlEnum control, Integer speed) {
        validateDeviceId(deviceId, "控制设备云台时设备ID不能为空");
        validateNotNull(control, "云台控制枚举不能为空");

        if (speed != null && (speed < 1 || speed > 255)) {
            throw new IllegalArgumentException("云台控制速度必须在1-255之间");
        }

        int finalSpeed = speed != null ? speed : DEFAULT_SPEED;
        return executeCommand("controlDevicePtz", deviceId,
            () -> serverCommandSender.deviceControlPtzCmd(deviceId, control, finalSpeed),
            deviceId, control, finalSpeed);
    }

    // ==================== 方向控制指令 ====================

    /**
     * 向上移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUp(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.TILT_UP, speed);
    }

    /**
     * 向下移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDown(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.TILT_DOWN, speed);
    }

    /**
     * 向左移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveLeft(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_LEFT, speed);
    }

    /**
     * 向右移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveRight(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_RIGHT, speed);
    }

    /**
     * 向左上移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUpLeft(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_LEFT_TILT_UP, speed);
    }

    /**
     * 向右上移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUpRight(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_RIGHT_TILT_UP, speed);
    }

    /**
     * 向左下移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDownLeft(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_LEFT_TILT_DOWN, speed);
    }

    /**
     * 向右下移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDownRight(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.PAN_RIGHT_TILT_DOWN, speed);
    }

    // ==================== 变焦控制指令 ====================

    /**
     * 放大变焦
     *
     * @param deviceId 设备ID
     * @param speed 变焦速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> zoomIn(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.ZOOM_IN, speed);
    }

    /**
     * 缩小变焦
     *
     * @param deviceId 设备ID
     * @param speed 变焦速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> zoomOut(String deviceId, Integer speed) {
        return controlDevicePtz(deviceId, PTZControlEnum.ZOOM_OUT, speed);
    }

    // ==================== 停止控制指令 ====================

    /**
     * 停止云台移动
     * <p>
     * 发送停止指令，停止当前的云台移动或变焦操作。
     * </p>
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> stopDevicePtz(String deviceId) {
        validateDeviceId(deviceId, "停止设备云台时设备ID不能为空");

        return executeCommand("stopDevicePtz", deviceId,
            () -> serverCommandSender.deviceControlPtzCmd(deviceId, PTZControlEnum.STOP, 0),
            deviceId);
    }

    // ==================== 便捷方法（使用默认速度） ====================

    /**
     * 使用默认速度进行方向控制
     * <p>
     * 使用默认速度（128）进行方向控制的便捷方法。
     * </p>
     *
     * @param deviceId 设备ID
     * @param control 云台控制枚举
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> controlWithDefaultSpeed(String deviceId, PTZControlEnum control) {
        return controlDevicePtz(deviceId, control, DEFAULT_SPEED);
    }

    /**
     * 向上移动（默认速度）
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUp(String deviceId) {
        return moveUp(deviceId, DEFAULT_SPEED);
    }

    /**
     * 向下移动（默认速度）
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDown(String deviceId) {
        return moveDown(deviceId, DEFAULT_SPEED);
    }

    /**
     * 向左移动（默认速度）
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveLeft(String deviceId) {
        return moveLeft(deviceId, DEFAULT_SPEED);
    }

    /**
     * 向右移动（默认速度）
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveRight(String deviceId) {
        return moveRight(deviceId, DEFAULT_SPEED);
    }

    /**
     * 放大变焦（默认速度）
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> zoomIn(String deviceId) {
        return zoomIn(deviceId, DEFAULT_SPEED);
    }

    /**
     * 缩小变焦（默认速度）
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> zoomOut(String deviceId) {
        return zoomOut(deviceId, DEFAULT_SPEED);
    }
}
