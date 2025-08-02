package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端告警查询指令实现类
 * <p>
 * 提供告警查询相关的指令发送功能，包括告警信息查询、告警控制等操作。
 * 继承AbstractVoglanderServerCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的告警操作</h3>
 * <ul>
 * <li>告警信息查询 - 按时间范围和优先级查询告警信息</li>
 * <li>告警控制 - 设置告警方式和类型</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderServerAlarmCommand alarmCommand;
 * 
 * // 查询告警信息
 * Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
 * Date endTime = new Date();
 * ResultDTO<Void> result1 = alarmCommand.queryDeviceAlarm("34020000001320000001",
 *     startTime, endTime, "1", "4", "5", "1");
 * 
 * // 设置告警控制
 * ResultDTO<Void> result2 = alarmCommand.controlDeviceAlarm("34020000001320000001", "5", "1");
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Component
public class VoglanderServerAlarmCommand extends AbstractVoglanderServerCommand {

    /**
     * 查询设备告警信息
     * <p>
     * 向设备发送告警信息查询指令，获取指定时间范围和优先级的告警信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param startPriority 开始优先级（1-紧急报警，2-设备报警，3-设备故障，4-设备恢复）
     * @param endPriority 结束优先级
     * @param alarmMethod 告警方式（1-现场报警，2-网络报警，3-移动报警，4-语音报警，5-视频报警，6-短信报警，7-邮件报警，8-FTP上传报警，9-HTTP上传报警）
     * @param alarmType 告警类型（1-视频丢失报警，2-设备防拆报警，3-存储设备磁盘满报警，4-设备高温报警，5-设备低温报警）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> queryDeviceAlarm(String deviceId, Date startTime, Date endTime,
        String startPriority, String endPriority,
        String alarmMethod, String alarmType) {
        validateDeviceId(deviceId, "查询设备告警信息时设备ID不能为空");
        validateNotNull(startTime, "开始时间不能为空");
        validateNotNull(endTime, "结束时间不能为空");

        return executeCommand("queryDeviceAlarm", deviceId,
            () -> ServerCommandSender.deviceAlarmQuery(getServerFromDevice(), getToDevice(deviceId),
                startTime, endTime, startPriority, endPriority, alarmMethod, alarmType),
            deviceId, startTime, endTime, startPriority, endPriority, alarmMethod, alarmType);
    }

    /**
     * 控制设备告警
     * <p>
     * 向设备发送告警控制指令，设置告警方式和类型。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param alarmMethod 告警方式（1-现场报警，2-网络报警，3-移动报警，4-语音报警，5-视频报警，6-短信报警，7-邮件报警，8-FTP上传报警，9-HTTP上传报警）
     * @param alarmType 告警类型（1-视频丢失报警，2-设备防拆报警，3-存储设备磁盘满报警，4-设备高温报警，5-设备低温报警）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> controlDeviceAlarm(String deviceId, String alarmMethod, String alarmType) {
        validateDeviceId(deviceId, "控制设备告警时设备ID不能为空");
        validateNotNull(alarmMethod, "告警方式不能为空");
        validateNotNull(alarmType, "告警类型不能为空");

        return executeCommand("controlDeviceAlarm", deviceId,
            () -> ServerCommandSender.deviceControlAlarm(getServerFromDevice(), getToDevice(deviceId), alarmMethod, alarmType),
            deviceId, alarmMethod, alarmType);
    }

    /**
     * 查询今日告警信息
     * <p>
     * 查询设备今日的告警信息（所有优先级和类型）。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryTodayDeviceAlarm(String deviceId) {
        validateDeviceId(deviceId, "查询今日告警信息时设备ID不能为空");

        long now = System.currentTimeMillis();
        long todayStart = now - (now % (24 * 60 * 60 * 1000)); // 今日0点

        Date startTime = new Date(todayStart);
        Date endTime = new Date(now);

        return queryDeviceAlarm(deviceId, startTime, endTime, "1", "4", null, null);
    }

    /**
     * 查询紧急告警信息
     * <p>
     * 查询设备指定时间范围内的紧急告警（优先级1）。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryEmergencyDeviceAlarm(String deviceId, Date startTime, Date endTime) {
        return queryDeviceAlarm(deviceId, startTime, endTime, "1", "1", null, null);
    }

    /**
     * 查询设备故障告警
     * <p>
     * 查询设备指定时间范围内的设备故障告警（优先级3）。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryDeviceFaultAlarm(String deviceId, Date startTime, Date endTime) {
        return queryDeviceAlarm(deviceId, startTime, endTime, "3", "3", null, null);
    }

    /**
     * 查询视频丢失告警
     * <p>
     * 查询设备指定时间范围内的视频丢失告警。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryVideoLossAlarm(String deviceId, Date startTime, Date endTime) {
        return queryDeviceAlarm(deviceId, startTime, endTime, null, null, null, "1");
    }

    /**
     * 查询设备防拆告警
     * <p>
     * 查询设备指定时间范围内的设备防拆告警。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryTamperAlarm(String deviceId, Date startTime, Date endTime) {
        return queryDeviceAlarm(deviceId, startTime, endTime, null, null, null, "2");
    }

    /**
     * 启用网络告警
     * <p>
     * 设置设备使用网络方式进行告警。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param alarmType 告警类型
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> enableNetworkAlarm(String deviceId, String alarmType) {
        return controlDeviceAlarm(deviceId, "2", alarmType);
    }

    /**
     * 启用视频告警
     * <p>
     * 设置设备使用视频方式进行告警。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param alarmType 告警类型
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> enableVideoAlarm(String deviceId, String alarmType) {
        return controlDeviceAlarm(deviceId, "5", alarmType);
    }

    /**
     * 查询最近N小时的告警信息
     * <p>
     * 查询设备最近N小时内的告警信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param hours 小时数，必须大于0
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryRecentDeviceAlarm(String deviceId, int hours) {
        validateDeviceId(deviceId, "查询最近告警信息时设备ID不能为空");

        if (hours <= 0) {
            throw new IllegalArgumentException("小时数必须大于0");
        }

        long now = System.currentTimeMillis();
        long startTime = now - hours * 60 * 60 * 1000L;

        return queryDeviceAlarm(deviceId, new Date(startTime), new Date(now), "1", "4", null, null);
    }
}