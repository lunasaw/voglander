package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端告警查询指令实现类
 *
 * <h3>sip-gateway 1.8.0 envelope 改造（2026-06-01）</h3>
 * <ul>
 * <li>{@code gb28181.Query.AlarmQuery}（白名单）：payload {@code {startTime: long, endTime: long,
 * startPriority?, endPriority?, alarmMethod?, alarmType?}}</li>
 * <li>{@code gb28181.Control.AlarmReset}（白名单）：payload {@code {alarmMethod?, alarmType?}}
 * （cmdCode 为 ResetAlarm 由网关层固定）</li>
 * </ul>
 *
 * @author luna
 * @since 2025/8/2
 * @version 2.0
 */
@Component
public class VoglanderServerAlarmCommand extends AbstractVoglanderServerCommand {

    private static final String TYPE_ALARM_QUERY = "gb28181.Query.AlarmQuery";
    private static final String TYPE_ALARM_RESET = "gb28181.Control.AlarmReset";

    /**
     * 查询设备告警信息。
     */
    public ResultDTO<Void> queryDeviceAlarm(String deviceId, Date startTime, Date endTime,
        String startPriority, String endPriority,
        String alarmMethod, String alarmType) {
        validateDeviceId(deviceId, "查询设备告警信息时设备ID不能为空");
        validateNotNull(startTime, "开始时间不能为空");
        validateNotNull(endTime, "结束时间不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("startTime", startTime.getTime());
        payload.put("endTime", endTime.getTime());
        if (startPriority != null) {
            payload.put("startPriority", startPriority);
        }
        if (endPriority != null) {
            payload.put("endPriority", endPriority);
        }
        if (alarmMethod != null) {
            payload.put("alarmMethod", alarmMethod);
        }
        if (alarmType != null) {
            payload.put("alarmType", alarmType);
        }
        return dispatchEnvelope(TYPE_ALARM_QUERY, deviceId, payload);
    }

    /**
     * 控制设备告警（报警复位）。
     */
    public ResultDTO<Void> controlDeviceAlarm(String deviceId, String alarmMethod, String alarmType) {
        validateDeviceId(deviceId, "控制设备告警时设备ID不能为空");
        validateNotNull(alarmMethod, "告警方式不能为空");
        validateNotNull(alarmType, "告警类型不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("alarmMethod", alarmMethod);
        payload.put("alarmType", alarmType);
        return dispatchEnvelope(TYPE_ALARM_RESET, deviceId, payload);
    }

    // ==================== 便捷方法 ====================

    public ResultDTO<Void> queryTodayDeviceAlarm(String deviceId) {
        validateDeviceId(deviceId, "查询今日告警信息时设备ID不能为空");
        long now = System.currentTimeMillis();
        long todayStart = now - (now % (24 * 60 * 60 * 1000));
        return queryDeviceAlarm(deviceId, new Date(todayStart), new Date(now), "1", "4", null, null);
    }

    public ResultDTO<Void> queryEmergencyDeviceAlarm(String deviceId, Date startTime, Date endTime) {
        return queryDeviceAlarm(deviceId, startTime, endTime, "1", "1", null, null);
    }

    public ResultDTO<Void> queryDeviceFaultAlarm(String deviceId, Date startTime, Date endTime) {
        return queryDeviceAlarm(deviceId, startTime, endTime, "3", "3", null, null);
    }

    public ResultDTO<Void> queryVideoLossAlarm(String deviceId, Date startTime, Date endTime) {
        return queryDeviceAlarm(deviceId, startTime, endTime, null, null, null, "1");
    }

    public ResultDTO<Void> queryTamperAlarm(String deviceId, Date startTime, Date endTime) {
        return queryDeviceAlarm(deviceId, startTime, endTime, null, null, null, "2");
    }

    public ResultDTO<Void> enableNetworkAlarm(String deviceId, String alarmType) {
        return controlDeviceAlarm(deviceId, "2", alarmType);
    }

    public ResultDTO<Void> enableVideoAlarm(String deviceId, String alarmType) {
        return controlDeviceAlarm(deviceId, "5", alarmType);
    }

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
