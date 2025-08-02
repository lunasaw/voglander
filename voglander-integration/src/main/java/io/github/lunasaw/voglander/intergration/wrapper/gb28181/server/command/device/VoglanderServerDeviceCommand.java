package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端设备查询指令实现类
 * <p>
 * 提供设备基础信息查询相关的指令发送功能，包括设备信息、状态、目录、预设位等查询操作。
 * 继承AbstractVoglanderServerCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的设备查询</h3>
 * <ul>
 * <li>设备信息查询 - 设备基本信息（厂商、型号等）</li>
 * <li>设备状态查询 - 设备在线状态、GPS位置等</li>
 * <li>设备目录查询 - 设备通道列表</li>
 * <li>设备预设位查询 - 云台预设位信息</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderServerDeviceCommand deviceCommand;
 * 
 * // 查询设备信息
 * ResultDTO<Void> result1 = deviceCommand.queryDeviceInfo("34020000001320000001");
 * 
 * // 查询设备状态
 * ResultDTO<Void> result2 = deviceCommand.queryDeviceStatus("34020000001320000001");
 * 
 * // 查询设备目录
 * ResultDTO<Void> result3 = deviceCommand.queryDeviceCatalog("34020000001320000001");
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Component
public class VoglanderServerDeviceCommand extends AbstractVoglanderServerCommand {

    /**
     * 查询设备信息
     * <p>
     * 向设备发送信息查询指令，获取设备的基本信息如厂商、型号、软件版本等。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> queryDeviceInfo(String deviceId) {
        validateDeviceId(deviceId, "查询设备信息时设备ID不能为空");

        return executeCommand("queryDeviceInfo", deviceId,
            () -> ServerCommandSender.deviceInfoQuery(getServerFromDevice(), getToDevice(deviceId)),
            deviceId);
    }

    /**
     * 查询设备状态
     * <p>
     * 向设备发送状态查询指令，获取设备的在线状态、GPS位置、报警状态等信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> queryDeviceStatus(String deviceId) {
        validateDeviceId(deviceId, "查询设备状态时设备ID不能为空");

        return executeCommand("queryDeviceStatus", deviceId,
            () -> ServerCommandSender.deviceStatusQuery(getServerFromDevice(), getToDevice(deviceId)),
            deviceId);
    }

    /**
     * 查询设备目录
     * <p>
     * 向设备发送目录查询指令，获取设备的通道列表信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> queryDeviceCatalog(String deviceId) {
        validateDeviceId(deviceId, "查询设备目录时设备ID不能为空");

        return executeCommand("queryDeviceCatalog", deviceId,
            () -> ServerCommandSender.deviceCatalogQuery(getServerFromDevice(), getToDevice(deviceId)),
            deviceId);
    }

    /**
     * 查询设备预设位
     * <p>
     * 向设备发送预设位查询指令，获取云台的预设位信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> queryDevicePreset(String deviceId) {
        validateDeviceId(deviceId, "查询设备预设位时设备ID不能为空");

        return executeCommand("queryDevicePreset", deviceId,
            () -> ServerCommandSender.devicePresetQuery(getServerFromDevice(), getToDevice(deviceId)),
            deviceId);
    }

    /**
     * 查询移动设备位置
     * <p>
     * 向移动设备发送位置查询指令，获取GPS位置信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param interval 查询间隔（秒），可以为空（使用默认值）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> queryDeviceMobilePosition(String deviceId, String interval) {
        validateDeviceId(deviceId, "查询移动设备位置时设备ID不能为空");

        String queryInterval = interval != null ? interval : "5";
        return executeCommand("queryDeviceMobilePosition", deviceId,
            () -> ServerCommandSender.deviceMobilePositionQuery(getServerFromDevice(), getToDevice(deviceId), queryInterval),
            deviceId, queryInterval);
    }

    /**
     * 查询移动设备位置（使用默认间隔）
     * <p>
     * 使用默认查询间隔（5秒）查询移动设备位置。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryDeviceMobilePosition(String deviceId) {
        return queryDeviceMobilePosition(deviceId, null);
    }
}