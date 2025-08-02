package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.ptz;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.utils.PtzCmdEnum;
import io.github.lunasaw.gb28181.common.entity.utils.PtzUtils;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.AbstractVoglanderClientCommand;

/**
 * GB28181客户端云台控制指令实现类
 * <p>
 * 提供云台控制相关的指令发送功能，包括云台移动、变焦、预置位等控制操作。
 * 继承AbstractVoglanderClientCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的云台控制</h3>
 * <ul>
 * <li>方向控制 - 上、下、左、右、左上、右上、左下、右下</li>
 * <li>变焦控制 - 放大、缩小</li>
 * <li>预置位控制 - 设置、调用、删除预置位</li>
 * <li>自定义云台指令</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderClientPtzCommand ptzCommand;
 * 
 * // 向上移动，速度为5
 * ResultDTO<Void> result = ptzCommand.moveUp("34020000001320000001", 5);
 * 
 * // 放大变焦，速度为3
 * ResultDTO<Void> result2 = ptzCommand.zoomIn("34020000001320000001", 3);
 * 
 * // 停止云台移动
 * ResultDTO<Void> result3 = ptzCommand.stopMove("34020000001320000001");
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
public class VoglanderClientPtzCommand extends AbstractVoglanderClientCommand {

    /**
     * 发送自定义云台控制指令
     * <p>
     * 发送指定的云台控制命令字符串到设备。
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

        return executeCommand("sendPtzControlCommand", deviceId,
            () -> ClientCommandSender.sendInvitePlayControlCommand(getClientFromDevice(), getToDevice(deviceId), ptzCmd),
            ptzCmd);
    }

    /**
     * 发送云台控制指令（使用枚举）
     * <p>
     * 使用预定义的云台控制枚举和速度参数发送控制指令。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param ptzCmdEnum 云台控制命令枚举
     * @param speed 控制速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> sendPtzControlCommand(String deviceId, PtzCmdEnum ptzCmdEnum, int speed) {
        validateDeviceId(deviceId, "发送云台控制指令时设备ID不能为空");
        validateNotNull(ptzCmdEnum, "云台控制命令枚举不能为空");

        if (speed < 1 || speed > 255) {
            throw new IllegalArgumentException("云台控制速度必须在1-255之间");
        }

        String ptzCmd = PtzUtils.getPtzCmd(ptzCmdEnum, speed);
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
        return sendPtzControlCommand(deviceId, PtzCmdEnum.UP, speed);
    }

    /**
     * 向下移动
     * 
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDown(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PtzCmdEnum.DOWN, speed);
    }

    /**
     * 向左移动
     * 
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveLeft(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PtzCmdEnum.LEFT, speed);
    }

    /**
     * 向右移动
     * 
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveRight(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PtzCmdEnum.RIGHT, speed);
    }

    /**
     * 向左上移动
     * 
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUpLeft(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PtzCmdEnum.UPLEFT, speed);
    }

    /**
     * 向右上移动
     * 
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveUpRight(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PtzCmdEnum.UPRIGHT, speed);
    }

    /**
     * 向左下移动
     * 
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDownLeft(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PtzCmdEnum.DOWNLEFT, speed);
    }

    /**
     * 向右下移动
     * 
     * @param deviceId 设备ID
     * @param speed 移动速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveDownRight(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PtzCmdEnum.DOWNRIGHT, speed);
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
        return sendPtzControlCommand(deviceId, PtzCmdEnum.ZOOMIN, speed);
    }

    /**
     * 缩小变焦
     * 
     * @param deviceId 设备ID
     * @param speed 变焦速度（1-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> zoomOut(String deviceId, int speed) {
        return sendPtzControlCommand(deviceId, PtzCmdEnum.ZOOMOUT, speed);
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
        // 停止指令：所有方向和变焦都设置为0
        String stopCmd = PtzUtils.getPtzCmd(0, 0, 0, 0);
        return sendPtzControlCommand(deviceId, stopCmd);
    }

    // ==================== 高级控制方法 ====================

    /**
     * 自定义云台控制
     * <p>
     * 提供完全自定义的云台控制参数，支持独立设置水平、垂直和变焦速度。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param cmdCode 命令代码
     * @param horizonSpeed 水平速度（0-255）
     * @param verticalSpeed 垂直速度（0-255）
     * @param zoomSpeed 变焦速度（0-255）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> customPtzControl(String deviceId, int cmdCode, int horizonSpeed, int verticalSpeed, int zoomSpeed) {
        validateDeviceId(deviceId, "发送自定义云台控制指令时设备ID不能为空");

        if (horizonSpeed < 0 || horizonSpeed > 255 ||
            verticalSpeed < 0 || verticalSpeed > 255 ||
            zoomSpeed < 0 || zoomSpeed > 255) {
            throw new IllegalArgumentException("云台速度参数必须在0-255之间");
        }

        String ptzCmd = PtzUtils.getPtzCmd(cmdCode, horizonSpeed, verticalSpeed, zoomSpeed);
        return sendPtzControlCommand(deviceId, ptzCmd);
    }

    /**
     * 默认速度的方向控制
     * <p>
     * 使用默认速度（128）进行方向控制的便捷方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param ptzCmdEnum 云台控制命令枚举
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> moveWithDefaultSpeed(String deviceId, PtzCmdEnum ptzCmdEnum) {
        return sendPtzControlCommand(deviceId, ptzCmdEnum, 128);
    }
}