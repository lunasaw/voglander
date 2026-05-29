package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.ptz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;
import com.luna.common.text.RandomStrUtil;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.control.instruction.builder.PTZInstructionBuilder;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.transmit.cmd.CommandContext;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.AbstractVoglanderClientCommand;

/**
 * GB28181客户端云台控制指令实现类
 * <p>
 * 提供云台控制相关的指令发送功能，包括云台移动、变焦等控制操作。
 * 继承AbstractVoglanderClientCommand获得统一的异常处理和日志记录能力。
 * </p>
 *
 * <h3>sip-gateway 1.8.0 适配</h3>
 * <ul>
 * <li>移除了 {@code PtzCmdEnum}/{@code PtzUtils}，方向/变焦统一用
 * {@link PTZControlEnum} + {@link PTZInstructionBuilder} 生成 16 进制指令。</li>
 * <li>移除了 {@code ClientCommandSender.sendCommand(...)} 静态方法，改为注入
 * {@link ClientCommandSender} 实例并经 {@code send(CommandContext)} 下发 MESSAGE。</li>
 * </ul>
 *
 * <h3>支持的云台控制</h3>
 * <ul>
 * <li>方向控制 - 上、下、左、右、左���、右上、左下、右下</li>
 * <li>变焦控制 - 放大、缩小</li>
 * <li>自定义云台指令（16进制字节串）</li>
 * </ul>
 *
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
public class VoglanderClientPtzCommand extends AbstractVoglanderClientCommand {

    /**
     * 默认云台控制速度。
     */
    private static final int DEFAULT_SPEED = 128;

    /**
     * PTZ 指令默认地址。
     */
    private static final int PTZ_ADDRESS = 0x001;

    @Autowired
    private ClientCommandSender clientCommandSender;

    /**
     * 发送自定义云台控制指令
     * <p>
     * 发送指定的云台控制命令字符串到设备，将命令封装为 DeviceControlPtz 对象发送。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param ptzCmd 云台控制命令字符串（16进制格式）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或控制命令为空时抛出
     */
    public ResultDTO<Void> sendPtzControlCommand(String deviceId, String ptzCmd) {
        validateDeviceId(deviceId, "发送云台控制指令时设备ID不能为空");
        validateNotNull(ptzCmd, "云台控制命令不能为空");

        return executeCommand("sendPtzControlCommand", deviceId, () -> {
            // 创建 DeviceControlPtz 对象
            DeviceControlPtz deviceControlPtz = new DeviceControlPtz(
                CmdTypeEnum.DEVICE_CONTROL.getType(),
                RandomStrUtil.getValidationCode(),
                deviceId);
            deviceControlPtz.setPtzCmd(ptzCmd);
            deviceControlPtz.setPtzInfo(new DeviceControlPtz.PtzInfo());

            // 1.8.0：经实例 send(CommandContext) 下发 MESSAGE
            return clientCommandSender.send(CommandContext.builder()
                .role("client").commandType("MESSAGE")
                .fromDevice(getClientFromDevice()).toDevice(getToDevice(deviceId))
                .body(deviceControlPtz).build());
        }, ptzCmd);
    }

    /**
     * 发送云台控制指令（使用枚举）
     * <p>
     * 使用预定义的云台控制枚举和速度参数发送控制指令。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param control 云台控制枚举
     * @param speed 控制速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> sendPtzControlCommand(String deviceId, PTZControlEnum control, int speed) {
        validateDeviceId(deviceId, "发送云台控制指令时设备ID不能为空");
        validateNotNull(control, "云台控制枚举不能为空");

        if (speed < 1 || speed > 255) {
            throw new IllegalArgumentException("云台控制速度必须在1-255之间");
        }

        String ptzCmd = PTZInstructionBuilder.create()
            .address(PTZ_ADDRESS)
            .addPTZControl(control)
            .horizontalSpeed(Math.min(speed, 0xFF))
            .verticalSpeed(Math.min(speed, 0xFF))
            .zoomSpeed(Math.min(speed, 0xF))
            .buildToHex();
        return sendPtzControlCommand(deviceId, ptzCmd);
    }

    // ==================== 方向控制指令 ====================

    /**
     * 向上移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUp(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.TILT_UP, speed);
    }

    /**
     * 向下移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDown(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.TILT_DOWN, speed);
    }

    /**
     * 向左移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveLeft(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.PAN_LEFT, speed);
    }

    /**
     * 向右移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveRight(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.PAN_RIGHT, speed);
    }

    /**
     * 向左上移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUpLeft(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.PAN_LEFT_TILT_UP, speed);
    }

    /**
     * 向右上移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUpRight(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.PAN_RIGHT_TILT_UP, speed);
    }

    /**
     * 向左下移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDownLeft(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.PAN_LEFT_TILT_DOWN, speed);
    }

    /**
     * 向右下移动
     *
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDownRight(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.PAN_RIGHT_TILT_DOWN, speed);
    }

    // ==================== 变焦控制指令 ====================

    /**
     * 放大变焦
     *
     * @param deviceId 设备ID
     * @param speed 变焦速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> zoomIn(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.ZOOM_IN, speed);
    }

    /**
     * 缩小变焦
     *
     * @param deviceId 设备ID
     * @param speed 变焦速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> zoomOut(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PTZControlEnum.ZOOM_OUT, speed);
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
    public ResultDTO<Void> stopMove(String deviceId) {
        String stopCmd = PTZInstructionBuilder.create()
            .address(PTZ_ADDRESS)
            .addPTZControl(PTZControlEnum.STOP)
            .buildToHex();
        return sendPtzControlCommand(deviceId, stopCmd);
    }

    // ==================== 高级控制方法 ====================

    /**
     * 默认速度的方向控制
     * <p>
     * 使用默认速度（128）进行方向控制的便捷方法。
     * </p>
     *
     * @param deviceId 设备ID
     * @param control 云台控制枚举
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveWithDefaultSpeed(String deviceId, PTZControlEnum control) {
        return sendPtzControlCommand(deviceId, control, DEFAULT_SPEED);
    }
}
