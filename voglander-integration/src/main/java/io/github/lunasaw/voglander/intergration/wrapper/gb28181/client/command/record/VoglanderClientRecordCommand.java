package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.record;

import java.util.List;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.response.DeviceRecord;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.AbstractVoglanderClientCommand;

/**
 * GB28181客户端录像控制指令实现类
 * <p>
 * 提供录像相关的指令发送功能，包括录像文件查询响应、录像控制等操作。
 * 继承AbstractVoglanderClientCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的录像操作</h3>
 * <ul>
 * <li>录像文件查询响应 - {@link DeviceRecord}</li>
 * <li>录像文件列表响应 - {@link DeviceRecord.RecordItem}</li>
 * <li>录像控制指令</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderClientRecordCommand recordCommand;
 * 
 * // 发送录像查询响应
 * DeviceRecord deviceRecord = new DeviceRecord();
 * deviceRecord.setName("录像查询");
 * deviceRecord.setSumNum(10);
 * ResultDTO<Void> result = recordCommand.sendDeviceRecordCommand("34020000001320000001", deviceRecord);
 * 
 * // 发送录像文件列表
 * List<DeviceRecord.RecordItem> recordItems = Arrays.asList(recordItem1, recordItem2);
 * ResultDTO<Void> result2 = recordCommand.sendRecordItemsCommand("34020000001320000001", recordItems);
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Component
public class VoglanderClientRecordCommand extends AbstractVoglanderClientCommand {

    /**
     * 发送录像查询响应指令
     * <p>
     * 向平台发送录像查询的响应信息，包含录像统计和详细信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceRecord 录像响应对象，包含录像查询结果
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或录像响应对象为空时抛出
     */
    public ResultDTO<Void> sendDeviceRecordCommand(String deviceId, DeviceRecord deviceRecord) {
        validateDeviceId(deviceId, "发送录像响应指令时设备ID不能为空");
        validateNotNull(deviceRecord, "录像响应对象不能为空");

        return executeCommand("sendDeviceRecordCommand", deviceId,
            () -> ClientCommandSender.sendDeviceRecordCommand(getClientFromDevice(), getToDevice(deviceId), deviceRecord),
            deviceRecord);
    }

    /**
     * 发送录像文件列表响应指令
     * <p>
     * 向平台发送录像文件列表，通常用于录像查询的详细响应。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceRecordItems 录像文件列表，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或录像文件列表为空时抛出
     */
    public ResultDTO<Void> sendRecordItemsCommand(String deviceId, List<DeviceRecord.RecordItem> deviceRecordItems) {
        validateDeviceId(deviceId, "发送录像文件列表指令时设备ID不能为空");
        validateNotNull(deviceRecordItems, "录像文件列表不能为空");

        return executeCommand("sendRecordItemsCommand", deviceId,
            () -> ClientCommandSender.sendDeviceRecordCommand(getClientFromDevice(), getToDevice(deviceId), deviceRecordItems),
            deviceRecordItems);
    }

    /**
     * 创建录像响应对象
     * <p>
     * 根据基础参数快速构建录像响应对象的工具方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param sn 查询名称
     * @param sumNum 录像文件总数
     * @param recordItems 录像文件列表
     * @return DeviceRecord 录像响应对象
     */
    public DeviceRecord createDeviceRecord(String deviceId, String sn, Integer sumNum, List<DeviceRecord.RecordItem> recordItems) {
        DeviceRecord deviceRecord = new DeviceRecord();
        deviceRecord.setDeviceId(deviceId);
        deviceRecord.setSn(sn != null ? sn : "录像查询");
        deviceRecord.setSumNum(sumNum != null ? sumNum : (recordItems != null ? recordItems.size() : 0));

        if (recordItems != null && !recordItems.isEmpty()) {
            deviceRecord.setRecordList(recordItems);
        }

        return deviceRecord;
    }

    /**
     * 创建录像文件项
     * <p>
     * 根据基础参数快速构建录像文件项的工具方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param name 录像文件名称
     * @param filePath 录像文件路径
     * @param address 录像存储地址
     * @param startTime 录像开始时间
     * @param endTime 录像结束时间
     * @param fileSize 录像文件大小
     * @param type 录像类型（time/alarm/manual/all）
     * @return DeviceRecord.RecordItem 录像文件项
     */
    public DeviceRecord.RecordItem createRecordItem(String deviceId, String name, String filePath,
        String startTime, String endTime, String fileSize, String type) {
        DeviceRecord.RecordItem recordItem = new DeviceRecord.RecordItem();
        recordItem.setDeviceId(deviceId);
        recordItem.setName(name);
        recordItem.setFilePath(filePath);
        recordItem.setStartTime(startTime);
        recordItem.setEndTime(endTime);
        recordItem.setFileSize(fileSize);
        recordItem.setType(type != null ? type : "time");
        return recordItem;
    }

    /**
     * 发送简化录像响应指令
     * <p>
     * 根据基础参数快速发送录像响应，适用于简单场景。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param queryName 查询名称
     * @param recordItems 录像文件列表
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendSimpleRecordResponse(String deviceId, String queryName, List<DeviceRecord.RecordItem> recordItems) {
        DeviceRecord deviceRecord = createDeviceRecord(deviceId, queryName, null, recordItems);
        return sendDeviceRecordCommand(deviceId, deviceRecord);
    }

    /**
     * 发送空录像响应指令
     * <p>
     * 发送一个表示没有录像文件的响应，通常用于查询无结果的情况。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param queryName 查询名称
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendEmptyRecordResponse(String deviceId, String queryName) {
        DeviceRecord deviceRecord = createDeviceRecord(deviceId, queryName, 0, null);
        return sendDeviceRecordCommand(deviceId, deviceRecord);
    }

    /**
     * 发送录像控制指令
     * <p>
     * 发送通用的录像控制内容，如开始录像、停止录像等。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param controlContent 控制内容
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或控制内容为空时抛出
     */
    public ResultDTO<Void> sendRecordControlCommand(String deviceId, String controlContent) {
        validateDeviceId(deviceId, "发送录像控制指令时设备ID不能为空");
        validateNotNull(controlContent, "录像控制内容不能为空");

        return executeCommand("sendRecordControlCommand", deviceId,
            () -> ClientCommandSender.sendInvitePlayControlCommand(getClientFromDevice(), getToDevice(deviceId), controlContent),
            controlContent);
    }

    /**
     * 开始录像指令
     * <p>
     * 发送开始录像的控制指令。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> startRecord(String deviceId) {
        return sendRecordControlCommand(deviceId, "RECORD");
    }

    /**
     * 停止录像指令
     * <p>
     * 发送停止录像的控制指令。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> stopRecord(String deviceId) {
        return sendRecordControlCommand(deviceId, "STOP");
    }
}