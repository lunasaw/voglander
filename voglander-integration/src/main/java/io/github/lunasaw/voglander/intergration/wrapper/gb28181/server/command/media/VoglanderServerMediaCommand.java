package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gbproxy.server.entity.InviteRequest;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端媒体流指令实现类
 * <p>
 * 提供媒体流相关的指令发送功能，包括实时流点播、回放流点播、流控制、会话控制等操作。
 * 继承AbstractVoglanderServerCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的媒体操作</h3>
 * <ul>
 * <li>实时流点播 - INVITE请求实时视频流</li>
 * <li>回放流点播 - INVITE请求历史录像回放</li>
 * <li>回放控制 - 播放、暂停、快进、慢放等控制</li>
 * <li>会话控制 - ACK确认、BYE结束会话</li>
 * <li>广播通知 - 设备广播消息</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderServerMediaCommand mediaCommand;
 * 
 * // 请求实时流
 * ResultDTO<Void> result1 = mediaCommand.inviteRealTimePlay("34020000001320000001",
 *     "192.168.1.100", 10000);
 * 
 * // 请求回放流
 * ResultDTO<Void> result2 = mediaCommand.invitePlayBack("34020000001320000001",
 *     "192.168.1.100", 10000, "2024-01-01T08:00:00", "2024-01-01T18:00:00");
 * 
 * // 控制回放播放
 * ResultDTO<Void> result3 = mediaCommand.controlPlayBack("34020000001320000001", PlayActionEnums.PLAY);
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Component
public class VoglanderServerMediaCommand extends AbstractVoglanderServerCommand {

    /**
     * 邀请设备实时流播放
     * <p>
     * 向设备发送INVITE请求，请求实时视频流。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param sdpIp SDP中的IP地址
     * @param mediaPort 媒体端口
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> inviteRealTimePlay(String deviceId, String sdpIp, Integer mediaPort) {
        validateDeviceId(deviceId, "邀请设备实时流播放时设备ID不能为空");
        validateNotNull(sdpIp, "SDP IP地址不能为空");
        validateNotNull(mediaPort, "媒体端口不能为空");

        return executeCommand("inviteRealTimePlay", deviceId,
            () -> ServerCommandSender.deviceInvitePlay(getServerFromDevice(), getToDevice(deviceId), sdpIp, mediaPort),
            deviceId, sdpIp, mediaPort);
    }

    /**
     * 邀请设备实时流播放（使用InviteRequest）
     * <p>
     * 使用完整的InviteRequest对象向设备发送INVITE请求。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param inviteRequest 邀请请求对象
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> inviteRealTimePlay(String deviceId, InviteRequest inviteRequest) {
        validateDeviceId(deviceId, "邀请设备实时流播放时设备ID不能为空");
        validateNotNull(inviteRequest, "邀请请求对象不能为空");

        return executeCommand("inviteRealTimePlay", deviceId,
            () -> ServerCommandSender.deviceInvitePlay(getServerFromDevice(), getToDevice(deviceId), inviteRequest),
            deviceId, inviteRequest);
    }

    /**
     * 邀请设备回放流播放
     * <p>
     * 向设备发送INVITE请求，请求历史录像回放流。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param sdpIp SDP中的IP地址
     * @param mediaPort 媒体端口
     * @param startTime 回放开始时间（格式：yyyy-MM-ddTHH:mm:ss）
     * @param endTime 回放结束时间（格式：yyyy-MM-ddTHH:mm:ss）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> invitePlayBack(String deviceId, String sdpIp, Integer mediaPort,
        String startTime, String endTime) {
        validateDeviceId(deviceId, "邀请设备回放流播放时设备ID不能为空");
        validateNotNull(sdpIp, "SDP IP地址不能为空");
        validateNotNull(mediaPort, "媒体端口不能为空");
        validateNotNull(startTime, "回放开始时间不能为空");
        validateNotNull(endTime, "回放结束时间不能为空");

        return executeCommand("invitePlayBack", deviceId,
            () -> ServerCommandSender.deviceInvitePlayBack(getServerFromDevice(), getToDevice(deviceId),
                sdpIp, mediaPort, startTime, endTime),
            deviceId, sdpIp, mediaPort, startTime, endTime);
    }

    /**
     * 邀请设备回放流播放（使用InviteRequest）
     * <p>
     * 使用完整的InviteRequest对象向设备发送回放INVITE请求。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param inviteRequest 邀请请求对象
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> invitePlayBack(String deviceId, InviteRequest inviteRequest) {
        validateDeviceId(deviceId, "邀请设备回放流播放时设备ID不能为空");
        validateNotNull(inviteRequest, "邀请请求对象不能为空");

        return executeCommand("invitePlayBack", deviceId,
            () -> ServerCommandSender.deviceInvitePlayBack(getServerFromDevice(), getToDevice(deviceId), inviteRequest),
            deviceId, inviteRequest);
    }

    /**
     * 控制设备回放播放
     * <p>
     * 向设备发送INFO请求，控制回放的播放状态（播放、暂停、快进等）。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param playAction 播放操作枚举
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> controlPlayBack(String deviceId, PlayActionEnums playAction) {
        validateDeviceId(deviceId, "控制设备回放播放时设备ID不能为空");
        validateNotNull(playAction, "播放操作不能为空");

        return executeCommand("controlPlayBack", deviceId,
            () -> ServerCommandSender.deviceInvitePlayBackControl(getServerFromDevice(), getToDevice(deviceId), playAction),
            deviceId, playAction);
    }

    /**
     * 发送ACK响应
     * <p>
     * 向设备发送ACK响应确认收到请求。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> sendAck(String deviceId) {
        validateDeviceId(deviceId, "发送ACK响应时设备ID不能为空");

        return executeCommand("sendAck", deviceId,
            () -> ServerCommandSender.deviceAck(getServerFromDevice(), getToDevice(deviceId)),
            deviceId);
    }

    /**
     * 发送ACK响应（指定callId）
     * <p>
     * 向设备发送ACK响应，指定特定的呼叫ID。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param callId 呼叫ID
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> sendAck(String deviceId, String callId) {
        validateDeviceId(deviceId, "发送ACK响应时设备ID不能为空");
        validateNotNull(callId, "呼叫ID不能为空");

        return executeCommand("sendAck", deviceId,
            () -> ServerCommandSender.deviceAck(getServerFromDevice(), getToDevice(deviceId), callId),
            deviceId, callId);
    }

    /**
     * 发送BYE请求
     * <p>
     * 向设备发送BYE请求，结束会话。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> sendBye(String deviceId) {
        validateDeviceId(deviceId, "发送BYE请求时设备ID不能为空");

        return executeCommand("sendBye", deviceId,
            () -> ServerCommandSender.deviceBye(getServerFromDevice(), getToDevice(deviceId)),
            deviceId);
    }

    /**
     * 发送设备广播
     * <p>
     * 向设备发送广播通知消息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> sendBroadcast(String deviceId) {
        validateDeviceId(deviceId, "发送设备广播时设备ID不能为空");

        return executeCommand("sendBroadcast", deviceId,
            () -> ServerCommandSender.deviceBroadcast(getServerFromDevice(), getToDevice(deviceId)),
            deviceId);
    }

    // ==================== 回放控制便捷方法 ====================

    /**
     * 播放回放
     * 
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> playBack(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.PLAY);
    }

    /**
     * 暂停回放
     * 
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> pauseBack(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.PAUSE);
    }

    /**
     * 停止回放
     * 
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> stopBack(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.TEARDOWN);
    }

    /**
     * 快进回放（2倍速）
     * 
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> fastForward(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.FAST);
    }

    /**
     * 慢放回放（0.5倍速）
     * 
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> slowPlay(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.SLOW);
    }

    // ==================== 便捷的实时流方法 ====================

    /**
     * 创建InviteRequest对象
     * <p>
     * 根据参数创建InviteRequest对象的工具方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param streamMode 流模式
     * @param sdpIp SDP IP地址
     * @param mediaPort 媒体端口
     * @return InviteRequest 邀请请求对象
     */
    public InviteRequest createInviteRequest(String deviceId, StreamModeEnum streamMode,
        String sdpIp, Integer mediaPort) {
        return new InviteRequest(deviceId, streamMode, sdpIp, mediaPort);
    }

    /**
     * 创建回放InviteRequest对象
     * <p>
     * 根据参数创建用于回放的InviteRequest对象的工具方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param streamMode 流模式
     * @param sdpIp SDP IP地址
     * @param mediaPort 媒体端口
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return InviteRequest 邀请请求对象
     */
    public InviteRequest createPlayBackInviteRequest(String deviceId, StreamModeEnum streamMode,
        String sdpIp, Integer mediaPort,
        String startTime, String endTime) {
        return new InviteRequest(deviceId, streamMode, sdpIp, mediaPort, startTime, endTime);
    }

    /**
     * 使用UDP流模式邀请实时播放
     * 
     * @param deviceId 设备ID
     * @param sdpIp SDP IP地址
     * @param mediaPort 媒体端口
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> inviteRealTimePlayUdp(String deviceId, String sdpIp, Integer mediaPort) {
        InviteRequest request = createInviteRequest(deviceId, StreamModeEnum.UDP, sdpIp, mediaPort);
        return inviteRealTimePlay(deviceId, request);
    }

    /**
     * 使用TCP流模式邀请实时播放
     * 
     * @param deviceId 设备ID
     * @param sdpIp SDP IP地址
     * @param mediaPort 媒体端口
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> inviteRealTimePlayTcp(String deviceId, String sdpIp, Integer mediaPort) {
        InviteRequest request = createInviteRequest(deviceId, StreamModeEnum.TCP, sdpIp, mediaPort);
        return inviteRealTimePlay(deviceId, request);
    }
}