package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.device;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceStatus;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.AbstractVoglanderClientCommand;

/**
 * GB28181客户端设备信息指令实现类
 * <p>
 * 提供设备信息查询和状态管理相关的指令发送功能，包括设备基本信息查询、设备状态上报等。
 * 继承AbstractVoglanderClientCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的设备操作</h3>
 * <ul>
 * <li>设备信息响应 - {@link DeviceInfo}</li>
 * <li>设备状态上报 - {@link DeviceStatus}</li>
 * <li>设备在线状态管理</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderClientDeviceCommand deviceCommand;
 * 
 * // 发送设备信息响应
 * DeviceInfo deviceInfo = new DeviceInfo();
 * deviceInfo.setDeviceName("摄像头001");
 * deviceInfo.setManufacturer("海康威视");
 * ResultDTO<Void> result = deviceCommand.sendDeviceInfoCommand("34020000001320000001", deviceInfo);
 * 
 * // 发送设备状态
 * ResultDTO<Void> result2 = deviceCommand.sendDeviceStatusCommand("34020000001320000001", "ONLINE");
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
public class VoglanderClientDeviceCommand extends AbstractVoglanderClientCommand {

    /**
     * 发送设备信息响应指令
     * <p>
     * 向平台上报设备的基本信息，包括设备名称、制造商、型号等详细信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceInfo 设备信息对象，包含设备详细信息
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或设备信息为空时抛出
     */
    public ResultDTO<Void> sendDeviceInfoCommand(String deviceId, DeviceInfo deviceInfo) {
        validateDeviceId(deviceId, "发送设备信息指令时设备ID不能为空");
        validateNotNull(deviceInfo, "设备信息不能为空");

        return executeCommand("sendDeviceInfoCommand", deviceId,
            () -> ClientCommandSender.sendDeviceInfoCommand(getClientFromDevice(), getToDevice(deviceId), deviceInfo),
            deviceInfo);
    }

    /**
     * 发送设备状态响应指令
     * <p>
     * 向平台上报设备的在线状态信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param online 在线状态："ONLINE"表示在线，"OFFLINE"表示离线
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或状态为空时抛出
     */
    public ResultDTO<Void> sendDeviceStatusCommand(String deviceId, String online) {
        validateDeviceId(deviceId, "发送设备状态指令时设备ID不能为空");
        validateNotNull(online, "设备状态不能为空");

        return executeCommand("sendDeviceStatusCommand", deviceId,
            () -> ClientCommandSender.sendDeviceStatusCommand(getClientFromDevice(), getToDevice(deviceId), online),
            online);
    }

    /**
     * 发送设备状态响应指令（使用DeviceStatus对象）
     * <p>
     * 向平台上报设备的详细状态信息，支持更丰富的状态数据。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceStatus 设备状态对象，包含详细状态信息
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或设备状态为空时抛出
     */
    public ResultDTO<Void> sendDeviceStatusCommand(String deviceId, DeviceStatus deviceStatus) {
        validateDeviceId(deviceId, "发送设备状态指令时设备ID不能为空");
        validateNotNull(deviceStatus, "设备状态对象不能为空");

        return executeCommand("sendDeviceStatusCommand", deviceId,
            () -> ClientCommandSender.sendDeviceStatusCommand(getClientFromDevice(), getToDevice(deviceId), deviceStatus),
            deviceStatus);
    }

    /**
     * 发送设备上线通知
     * <p>
     * 快捷方法，用于通知平台设备已上线。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendDeviceOnlineNotify(String deviceId) {
        return sendDeviceStatusCommand(deviceId, "ONLINE");
    }

    /**
     * 发送设备离线通知
     * <p>
     * 快捷方法，用于通知平台设备已离线。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendDeviceOfflineNotify(String deviceId) {
        return sendDeviceStatusCommand(deviceId, "OFFLINE");
    }

    /**
     * 创建基础设备信息对象
     * <p>
     * 根据基础参数快速构建设备信息对象的工具方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param deviceName 设备名称
     * @param manufacturer 制造商
     * @param model 设备型号
     * @param firmware 固件版本
     * @return DeviceInfo 设备信息对象
     */
    public DeviceInfo createDeviceInfo(String deviceId, String deviceName, String manufacturer, String model, String firmware) {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceName(deviceName);
        deviceInfo.setManufacturer(manufacturer);
        deviceInfo.setModel(model);
        deviceInfo.setFirmware(firmware);
        return deviceInfo;
    }

    /**
     * 发送简化设备信息指令
     * <p>
     * 根据基础参数快速发送设备信息，适用于简单场景。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceName 设备名称
     * @param manufacturer 制造商
     * @param model 设备型号
     * @param firmware 固件版本
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendSimpleDeviceInfoCommand(String deviceId, String deviceName, String manufacturer, String model, String firmware) {
        DeviceInfo deviceInfo = createDeviceInfo(deviceId, deviceName, manufacturer, model, firmware);
        return sendDeviceInfoCommand(deviceId, deviceInfo);
    }
}