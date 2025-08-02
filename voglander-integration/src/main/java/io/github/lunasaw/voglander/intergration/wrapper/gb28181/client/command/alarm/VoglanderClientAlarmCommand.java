package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.alarm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.AbstractVoglanderClientCommand;

/**
 * GB28181客户端告警指令实现类
 * <p>
 * 提供设备告警相关的指令发送功能，包括基础告警和告警通知。
 * 继承AbstractVoglanderClientCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的告警类型</h3>
 * <ul>
 * <li>设备基础告警 - {@link DeviceAlarm}</li>
 * <li>设备告警通知 - {@link DeviceAlarmNotify}</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderClientAlarmCommand alarmCommand;
 * 
 * // 发送基础告警
 * DeviceAlarm alarm = new DeviceAlarm();
 * alarm.setAlarmType("1");
 * alarm.setAlarmPriority("3");
 * ResultDTO<Void> result = alarmCommand.sendAlarmCommand("34020000001320000001", alarm);
 * 
 * // 发送告警通知
 * DeviceAlarmNotify notify = new DeviceAlarmNotify();
 * notify.setAlarmType("1");
 * ResultDTO<Void> result2 = alarmCommand.sendAlarmNotifyCommand("34020000001320000001", notify);
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
public class VoglanderClientAlarmCommand extends AbstractVoglanderClientCommand {

    /**
     * 发送设备基础告警指令
     * <p>
     * 向指定设备发送基础告警信息，包含告警类型、级别、时间、描述等基础信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceAlarm 告警信息对象，包含告警详细信息
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或告警信息为空时抛出
     */
    public ResultDTO<Void> sendAlarmCommand(String deviceId, DeviceAlarm deviceAlarm) {
        validateDeviceId(deviceId, "发送告警指令时设备ID不能为空");
        validateNotNull(deviceAlarm, "告警信息不能为空");

        return executeCommand("sendAlarmCommand", deviceId,
            () -> ClientCommandSender.sendAlarmCommand(getClientFromDevice(), getToDevice(deviceId), deviceAlarm),
            deviceAlarm);
    }

    /**
     * 发送设备告警通知指令
     * <p>
     * 向指定设备发送告警通知，通常用于更丰富的告警信息传递。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceAlarmNotify 告警通知对象，包含告警通知详细信息
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或告警通知为空时抛出
     */
    public ResultDTO<Void> sendAlarmNotifyCommand(String deviceId, DeviceAlarmNotify deviceAlarmNotify) {
        validateDeviceId(deviceId, "发送告警通知指令时设备ID不能为空");
        validateNotNull(deviceAlarmNotify, "告警通知信息不能为空");

        return executeCommand("sendAlarmNotifyCommand", deviceId,
            () -> ClientCommandSender.sendAlarmCommand(getClientFromDevice(), getToDevice(deviceId), deviceAlarmNotify),
            deviceAlarmNotify);
    }

    /**
     * 发送设备告警指令（简化版本）
     * <p>
     * 根据基础参数快速构建并发送告警信息，适用于简单场景。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param alarmType 告警类型（1-7：视频丢失、设备防拆、存储设备磁盘满、设备高温、设备低温、音频丢失、移动侦测）
     * @param alarmPriority 告警级别（1-4：一级警情、二级警情、三级警情、四级警情）
     * @param alarmMethod 告警方式（1-7）
     * @param alarmDescription 告警描述
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendSimpleAlarmCommand(String deviceId, String alarmType, String alarmPriority,
        String alarmMethod, String alarmDescription) {
        validateDeviceId(deviceId, "发送简化告警指令时设备ID不能为空");
        validateNotNull(alarmType, "告警类型不能为空");
        validateNotNull(alarmPriority, "告警级别不能为空");

        DeviceAlarm deviceAlarm = new DeviceAlarm();
        deviceAlarm.setDeviceId(deviceId);
        deviceAlarm.setAlarmType(alarmType);
        deviceAlarm.setAlarmPriority(alarmPriority);
        deviceAlarm.setAlarmMethod(alarmMethod != null ? alarmMethod : "1");
        deviceAlarm.setAlarmDescription(alarmDescription != null ? alarmDescription : "设备告警");
        deviceAlarm.setAlarmTime(new java.util.Date());

        return sendAlarmCommand(deviceId, deviceAlarm);
    }
}
