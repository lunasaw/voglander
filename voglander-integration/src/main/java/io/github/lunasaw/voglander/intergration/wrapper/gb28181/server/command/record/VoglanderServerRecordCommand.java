package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端录像查询指令实现类
 *
 * <h3>sip-gateway 1.8.0 envelope 改造（2026-06-01）</h3>
 * <ul>
 * <li>{@code gb28181.Query.RecordInfo}（白名单）：{@code {startTime: long, endTime: long}}（Number 类型）</li>
 * <li>{@code gb28181.Control.Record}（declare 表）：{@code {recordCmd: String}}</li>
 * </ul>
 *
 * <p>
 * 时间戳统一以毫秒 long 形式放入 payload，String 重载内部解析为毫秒。
 * </p>
 *
 * @author luna
 * @since 2025/8/2
 * @version 2.0
 */
@Component
public class VoglanderServerRecordCommand extends AbstractVoglanderServerCommand {

    private static final String TYPE_RECORD_INFO = "gb28181.Query.RecordInfo";
    private static final String TYPE_CTRL_RECORD = "gb28181.Control.Record";

    /**
     * 查询设备录像信息（毫秒时间戳）。
     */
    public ResultDTO<Void> queryDeviceRecord(String deviceId, long startTime, long endTime) {
        validateDeviceId(deviceId, "查询设备录像信息时设备ID不能为空");
        if (startTime <= 0 || endTime <= 0) {
            throw new IllegalArgumentException("时间戳必须大于0");
        }
        if (startTime >= endTime) {
            throw new IllegalArgumentException("开始时间必须小于结束时间");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        return dispatchEnvelope(TYPE_RECORD_INFO, deviceId, payload);
    }

    /**
     * 查询设备录像信息（Date 重载，转毫秒）。
     */
    public ResultDTO<Void> queryDeviceRecord(String deviceId, Date startTime, Date endTime) {
        validateDeviceId(deviceId, "查询设备录像信息时设备ID不能为空");
        validateNotNull(startTime, "开始时间不能为空");
        validateNotNull(endTime, "结束��间不能为空");
        return queryDeviceRecord(deviceId, startTime.getTime(), endTime.getTime());
    }

    /**
     * 查询设备录像信息（ISO 时间字符串重载，格式 yyyy-MM-ddTHH:mm:ss，按系统默认时区转毫秒）。
     */
    public ResultDTO<Void> queryDeviceRecord(String deviceId, String startTime, String endTime) {
        validateDeviceId(deviceId, "查询设备录像信息时设备ID不能为空");
        validateNotNull(startTime, "开始时间不能为空");
        validateNotNull(endTime, "结束时间不能为空");

        long startMs = parseIsoToEpochMilli(startTime);
        long endMs = parseIsoToEpochMilli(endTime);
        return queryDeviceRecord(deviceId, startMs, endMs);
    }

    /**
     * 控制设备录像。
     *
     * @param recordCmd Record（开始）/ StopRecord（停止）
     */
    public ResultDTO<Void> controlDeviceRecord(String deviceId, String recordCmd) {
        validateDeviceId(deviceId, "控制设备录像时设备ID不能为空");
        validateNotNull(recordCmd, "录像命令不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("recordCmd", recordCmd);
        return dispatchEnvelope(TYPE_CTRL_RECORD, deviceId, payload);
    }

    /**
     * 开始设备录像（recordCmd = Record）。
     */
    public ResultDTO<Void> startDeviceRecord(String deviceId) {
        return controlDeviceRecord(deviceId, "Record");
    }

    /**
     * 停止设备录像（recordCmd = StopRecord）。
     */
    public ResultDTO<Void> stopDeviceRecord(String deviceId) {
        return controlDeviceRecord(deviceId, "StopRecord");
    }

    /**
     * 解析 ISO 时间字符串为毫秒时间戳（按系统默认时区）。
     * 支持 {@code yyyy-MM-ddTHH:mm:ss} 与 {@code yyyy-MM-dd HH:mm:ss}。
     */
    private long parseIsoToEpochMilli(String iso) {
        String normalized = iso.contains("T") ? iso : iso.replace(' ', 'T');
        LocalDateTime ldt = LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        return instant.toEpochMilli();
    }
}
