package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.status;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.notify.DeviceKeepLiveNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MobilePositionNotify;
import io.github.lunasaw.gb28181.common.entity.notify.MediaStatusNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.AbstractVoglanderClientCommand;

/**
 * GB28181客户端设备状态指令实现类
 * <p>
 * 提供设备状态相关的指令发送功能，包括心跳保活、位置上报、媒体状态等操作。
 * 继承AbstractVoglanderClientCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的状态操作</h3>
 * <ul>
 * <li>心跳保活 - {@link DeviceKeepLiveNotify}</li>
 * <li>位置信息上报 - {@link MobilePositionNotify}</li>
 * <li>媒体状态通知 - {@link MediaStatusNotify}</li>
 * <li>设备状态上报</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderClientStatusCommand statusCommand;
 * 
 * // 发送心跳保活
 * ResultDTO<Void> result = statusCommand.sendKeepaliveCommand("34020000001320000001", "OK");
 * 
 * // 发送位置信息
 * MobilePositionNotify positionNotify = new MobilePositionNotify();
 * positionNotify.setLongitude(116.397128);
 * positionNotify.setLatitude(39.916527);
 * ResultDTO<Void> result2 = statusCommand.sendMobilePositionCommand("34020000001320000001", positionNotify);
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Component
public class VoglanderClientStatusCommand extends AbstractVoglanderClientCommand {

    /**
     * 发送心跳保活指令
     * <p>
     * 向平台发送设备心跳保活信息，维持设备在线状态。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param status 设备状态信息
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> sendKeepaliveCommand(String deviceId, String status) {
        validateDeviceId(deviceId, "发送心跳保活指令时设备ID不能为空");

        return executeCommand("sendKeepaliveCommand", deviceId,
            () -> ClientCommandSender.sendKeepaliveCommand(getClientFromDevice(), getToDevice(deviceId), status != null ? status : "OK"),
            status);
    }

    /**
     * 发送心跳保活指令（使用DeviceKeepLiveNotify对象）
     * <p>
     * 向平台发送详细的心跳保活信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceKeepLiveNotify 心跳保活通知对象，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或心跳通知对象为空时抛出
     */
    public ResultDTO<Void> sendKeepaliveCommand(String deviceId, DeviceKeepLiveNotify deviceKeepLiveNotify) {
        validateDeviceId(deviceId, "发送心跳保活指令时设备ID不能为空");
        validateNotNull(deviceKeepLiveNotify, "心跳保活通知对象不能为空");

        return executeCommand("sendKeepaliveCommand", deviceId,
            () -> ClientCommandSender.sendKeepaliveCommand(getClientFromDevice(), getToDevice(deviceId), deviceKeepLiveNotify),
            deviceKeepLiveNotify);
    }

    /**
     * 发送位置信息上报指令
     * <p>
     * 向平台上报设备的位置信息，用于移动设备的位置跟踪。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param mobilePositionNotify 位置通知对象，包含经纬度等信息，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或位置通知对象为空时抛出
     */
    public ResultDTO<Void> sendMobilePositionCommand(String deviceId, MobilePositionNotify mobilePositionNotify) {
        validateDeviceId(deviceId, "发送位置信息指令时设备ID不能为空");
        validateNotNull(mobilePositionNotify, "位置通知对象不能为空");

        return executeCommand("sendMobilePositionCommand", deviceId,
            () -> ClientCommandSender.sendMobilePositionNotify(getClientFromDevice(), getToDevice(deviceId), mobilePositionNotify),
            mobilePositionNotify);
    }

    /**
     * 发送媒体状态通知指令
     * <p>
     * 向平台发送媒体状态通知，报告媒体流的状态变化。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param notifyType 通知类型（如：121表示媒体通知）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> sendMediaStatusCommand(String deviceId, String notifyType) {
        validateDeviceId(deviceId, "发送媒体状态指令时设备ID不能为空");

        return executeCommand("sendMediaStatusCommand", deviceId,
            () -> ClientCommandSender.sendMediaStatusCommand(getClientFromDevice(), getToDevice(deviceId), notifyType != null ? notifyType : "121"),
            notifyType);
    }

    /**
     * 创建位置通知对象
     * <p>
     * 根据基础参数快速构建位置通知对象的工具方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param longitude 经度
     * @param latitude 纬度
     * @param time 时间戳
     * @param speed 速度
     * @param direction 方向
     * @param altitude 海拔高度
     * @return MobilePositionNotify 位置通知对象
     */
    public MobilePositionNotify createMobilePositionNotify(String deviceId, Double longitude, Double latitude,
        String time, Double speed, Double direction, Double altitude) {
        MobilePositionNotify positionNotify = new MobilePositionNotify();
        positionNotify.setDeviceId(deviceId);
        positionNotify.setLongitude(longitude);
        positionNotify.setLatitude(latitude);
        positionNotify.setTime(time);
        positionNotify.setSpeed(speed);
        positionNotify.setDirection(direction);
        positionNotify.setAltitude(altitude);
        return positionNotify;
    }

    /**
     * 创建简单位置通知对象
     * <p>
     * 使用最少参数创建位置通知对象的简化方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param longitude 经度
     * @param latitude 纬度
     * @return MobilePositionNotify 位置通知对象
     */
    public MobilePositionNotify createSimplePositionNotify(String deviceId, Double longitude, Double latitude) {
        return createMobilePositionNotify(deviceId, longitude, latitude, null, null, null, null);
    }

    /**
     * 发送简化位置信息指令
     * <p>
     * 根据经纬度快速发送位置信息，适用于简单场景。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param longitude 经度
     * @param latitude 纬度
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendSimplePositionCommand(String deviceId, Double longitude, Double latitude) {
        validateNotNull(longitude, "经度不能为空");
        validateNotNull(latitude, "纬度不能为空");

        MobilePositionNotify positionNotify = createSimplePositionNotify(deviceId, longitude, latitude);
        return sendMobilePositionCommand(deviceId, positionNotify);
    }

    /**
     * 发送设备正常状态心跳
     * <p>
     * 快捷方法，发送表示设备正常工作的心跳信号。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendNormalKeepalive(String deviceId) {
        return sendKeepaliveCommand(deviceId, "OK");
    }

    /**
     * 发送设备异常状态心跳
     * <p>
     * 快捷方法，发送表示设备异常状态的心跳信号。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param errorStatus 异常状态描述
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendErrorKeepalive(String deviceId, String errorStatus) {
        return sendKeepaliveCommand(deviceId, errorStatus != null ? errorStatus : "ERROR");
    }

    /**
     * 发送媒体流开始通知
     * <p>
     * 快捷方法，通知平台媒体流已开始。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendMediaStartNotify(String deviceId) {
        return sendMediaStatusCommand(deviceId, "121");
    }

    /**
     * 发送媒体流停止通知
     * <p>
     * 快捷方法，通知平台媒体流已停止。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendMediaStopNotify(String deviceId) {
        return sendMediaStatusCommand(deviceId, "122");
    }

    /**
     * 批量发送心跳保活指令
     * <p>
     * 向多个设备同时发送心跳保活指令。
     * </p>
     * 
     * @param deviceIds 设备ID列表，不能为空
     * @param status 设备状态信息
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendBatchKeepaliveCommand(java.util.List<String> deviceIds, String status) {
        validateNotNull(deviceIds, "设备ID列表不能为空");

        return executeCommand("sendBatchKeepaliveCommand",
            () -> {
                String lastCallId = null;
                for (String deviceId : deviceIds) {
                    lastCallId = ClientCommandSender.sendKeepaliveCommand(
                        getClientFromDevice(),
                        getToDevice(deviceId),
                        status != null ? status : "OK");
                }
                return lastCallId;
            },
            deviceIds, status);
    }
}