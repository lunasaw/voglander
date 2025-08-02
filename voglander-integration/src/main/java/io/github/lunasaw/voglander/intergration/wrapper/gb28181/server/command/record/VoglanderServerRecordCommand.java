package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端录像查询指令实现类
 * <p>
 * 提供录像查询相关的指令发送功能，包括录像信息查询、录像控制等操作。
 * 继承AbstractVoglanderServerCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的录像操作</h3>
 * <ul>
 * <li>录像信息查询 - 按时间范围查询录像文件</li>
 * <li>录像控制 - 开始录像、停止录像</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderServerRecordCommand recordCommand;
 * 
 * // 查询录像信息（时间字符串）
 * ResultDTO<Void> result1 = recordCommand.queryDeviceRecord("34020000001320000001",
 *     "2024-01-01T00:00:00", "2024-01-01T23:59:59");
 * 
 * // 查询录像信息（Date对象）
 * Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
 * Date endTime = new Date();
 * ResultDTO<Void> result2 = recordCommand.queryDeviceRecord("34020000001320000001", startTime, endTime);
 * 
 * // 开始录像
 * ResultDTO<Void> result3 = recordCommand.startDeviceRecord("34020000001320000001");
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Component
public class VoglanderServerRecordCommand extends AbstractVoglanderServerCommand {

    /**
     * 查询设备录像信息（时间字符串格式）
     * <p>
     * 向设备发送录像信息查询指令，获取指定时间范围内的录像文件信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param startTime 开始时间（格式：yyyy-MM-ddTHH:mm:ss）
     * @param endTime 结束时间（格式：yyyy-MM-ddTHH:mm:ss）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> queryDeviceRecord(String deviceId, String startTime, String endTime) {
        validateDeviceId(deviceId, "查询设备录像信息时设备ID不能为空");
        validateNotNull(startTime, "开始时间不能为空");
        validateNotNull(endTime, "结束时间不能为空");

        return executeCommand("queryDeviceRecord", deviceId,
            () -> ServerCommandSender.deviceRecordInfoQuery(getServerFromDevice(), getToDevice(deviceId), startTime, endTime),
            deviceId, startTime, endTime);
    }

    /**
     * 查询设备录像信息（Date对象格式）
     * <p>
     * 向设备发送录像信息查询指令，获取指定时间范围内的录像文件信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> queryDeviceRecord(String deviceId, Date startTime, Date endTime) {
        validateDeviceId(deviceId, "查询设备录像信息时设备ID不能为空");
        validateNotNull(startTime, "开始时间不能为空");
        validateNotNull(endTime, "结束时间不能为空");

        return executeCommand("queryDeviceRecord", deviceId,
            () -> ServerCommandSender.deviceRecordInfoQuery(getServerFromDevice(), getToDevice(deviceId), startTime, endTime),
            deviceId, startTime, endTime);
    }

    /**
     * 查询设备录像信息（时间戳格式）
     * <p>
     * 向设备发送录像信息查询指令，获取指定时间范围内的录像文件信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> queryDeviceRecord(String deviceId, long startTime, long endTime) {
        validateDeviceId(deviceId, "查询设备录像信息时设备ID不能为空");

        if (startTime <= 0 || endTime <= 0) {
            throw new IllegalArgumentException("时间戳必须大于0");
        }

        if (startTime >= endTime) {
            throw new IllegalArgumentException("开始时间必须小于结束时间");
        }

        return executeCommand("queryDeviceRecord", deviceId,
            () -> ServerCommandSender.deviceRecordInfoQuery(getServerFromDevice(), getToDevice(deviceId), startTime, endTime),
            deviceId, startTime, endTime);
    }

    /**
     * 控制设备录像
     * <p>
     * 向设备发送录像控制指令，可以开始或停止录像。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param recordCmd 录像命令（Record-开始录像，StopRecord-停止录像）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> controlDeviceRecord(String deviceId, String recordCmd) {
        validateDeviceId(deviceId, "控制设备录像时设备ID不能为空");
        validateNotNull(recordCmd, "录像命令不能为空");

        return executeCommand("controlDeviceRecord", deviceId,
            () -> ServerCommandSender.deviceControlRecord(getServerFromDevice(), getToDevice(deviceId), recordCmd),
            deviceId, recordCmd);
    }

    /**
     * 开始设备录像
     * <p>
     * 向设备发送开始录像指令。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> startDeviceRecord(String deviceId) {
        return controlDeviceRecord(deviceId, "Record");
    }

    /**
     * 停止设备录像
     * <p>
     * 向设备发送停止录像指令。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> stopDeviceRecord(String deviceId) {
        return controlDeviceRecord(deviceId, "StopRecord");
    }

    /**
     * 查询今日录像信息
     * <p>
     * 查询设备今日（当天0点到当前时间）的录像信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryTodayDeviceRecord(String deviceId) {
        validateDeviceId(deviceId, "查询今日录像信息时设备ID不能为空");

        long now = System.currentTimeMillis();
        long todayStart = now - (now % (24 * 60 * 60 * 1000)); // 今日0点

        return queryDeviceRecord(deviceId, todayStart, now);
    }

    /**
     * 查询昨日录像信息
     * <p>
     * 查询设备昨日（昨天0点到昨天23:59:59）的录像信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryYesterdayDeviceRecord(String deviceId) {
        validateDeviceId(deviceId, "查询昨日录像信息时设备ID不能为空");

        long now = System.currentTimeMillis();
        long todayStart = now - (now % (24 * 60 * 60 * 1000)); // 今日0点
        long yesterdayStart = todayStart - 24 * 60 * 60 * 1000; // 昨日0点
        long yesterdayEnd = todayStart - 1; // 昨日23:59:59.999

        return queryDeviceRecord(deviceId, yesterdayStart, yesterdayEnd);
    }

    /**
     * 查询最近N小时的录像信息
     * <p>
     * 查询设备最近N小时内的录像信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param hours 小时数，必须大于0
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> queryRecentDeviceRecord(String deviceId, int hours) {
        validateDeviceId(deviceId, "查询最近录像信息时设备ID不能为空");

        if (hours <= 0) {
            throw new IllegalArgumentException("小时数必须大于0");
        }

        long now = System.currentTimeMillis();
        long startTime = now - hours * 60 * 60 * 1000L;

        return queryDeviceRecord(deviceId, startTime, now);
    }
}