package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181服务端媒体流指令实现类
 * <p>
 * 提供媒体流相关的指令发送功能，包括实时流点播、回放流点播、流控制、会话控制等操作。
 * 继承AbstractVoglanderServerCommand获得统一的异常处理和日志记录能力。
 * </p>
 *
 * <h3>sip-gateway 1.8.0 适配</h3>
 * <ul>
 * <li>点播/回放 INVITE 改为按 {@code deviceId} 调用并显式传入 {@link StreamModeEnum}（默认 UDP）。</li>
 * <li>BYE 改为 dialog-aware，按 {@code callId} 调用 {@code serverCommandSender.deviceBye(callId)}。</li>
 * <li>1.8.0 移除了基于 {@code InviteRequest} 的重载与 {@code sendCommand("INFO", ...)}；
 * 回放倍速/Seek 等带数据的 INFO 控制不再由本包暴露（如需可经 envelope 白名单 cmdType 下发）。</li>
 * </ul>
 *
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Component
@Slf4j
public class VoglanderServerMediaCommand extends AbstractVoglanderServerCommand {

    /**
     * 邀请设备实时流播放（指定流模式）
     * <p>
     * 向设备发送INVITE请求，请求实时视频流。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param sdpIp SDP中的收流IP地址
     * @param mediaPort 收流媒体端口
     * @param streamMode 流传输模式
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> inviteRealTimePlay(String deviceId, String sdpIp, Integer mediaPort, StreamModeEnum streamMode) {
        validateDeviceId(deviceId, "邀请设备实时流播放时设备ID不能为空");
        validateNotNull(sdpIp, "SDP IP地址不能为空");
        validateNotNull(mediaPort, "媒体端口不能为空");
        validateNotNull(streamMode, "流传输模式不能为空");

        return executeCommand("inviteRealTimePlay", deviceId,
            () -> serverCommandSender.deviceInvitePlay(deviceId, sdpIp, mediaPort, streamMode),
            deviceId, sdpIp, mediaPort, streamMode);
    }

    /**
     * 邀请设备实时流播放（默认 UDP 流模式）
     *
     * @param deviceId 设备ID，不能为空
     * @param sdpIp SDP中的收流IP地址
     * @param mediaPort 收流媒体端口
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> inviteRealTimePlay(String deviceId, String sdpIp, Integer mediaPort) {
        return inviteRealTimePlay(deviceId, sdpIp, mediaPort, StreamModeEnum.UDP);
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
        return inviteRealTimePlay(deviceId, sdpIp, mediaPort, StreamModeEnum.UDP);
    }

    /**
     * 使用TCP主动流模式邀请实时播放
     *
     * @param deviceId 设备ID
     * @param sdpIp SDP IP地址
     * @param mediaPort 媒体端口
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> inviteRealTimePlayTcp(String deviceId, String sdpIp, Integer mediaPort) {
        return inviteRealTimePlay(deviceId, sdpIp, mediaPort, StreamModeEnum.TCP_ACTIVE);
    }

    /**
     * 邀请设备回放流播放（指定流模式）
     * <p>
     * 向设备发送INVITE请求，请求历史录像回放流。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param sdpIp SDP中的收流IP地址
     * @param mediaPort 收流媒体端口
     * @param streamMode 流传输模式
     * @param startTime 回放开始时间（格式：yyyy-MM-ddTHH:mm:ss）
     * @param endTime 回放结束时间（格式：yyyy-MM-ddTHH:mm:ss）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> invitePlayBack(String deviceId, String sdpIp, Integer mediaPort,
        StreamModeEnum streamMode, String startTime, String endTime) {
        validateDeviceId(deviceId, "邀请设备回放流播放时设备ID不能为空");
        validateNotNull(sdpIp, "SDP IP地址不能为空");
        validateNotNull(mediaPort, "媒体端口不能为空");
        validateNotNull(streamMode, "流传输模式不能为空");
        validateNotNull(startTime, "回放开始时间不能为空");
        validateNotNull(endTime, "回放结束时间不能为空");

        return executeCommand("invitePlayBack", deviceId,
            () -> serverCommandSender.deviceInvitePlayBack(deviceId, sdpIp, mediaPort, streamMode, startTime, endTime),
            deviceId, sdpIp, mediaPort, streamMode, startTime, endTime);
    }

    /**
     * 邀请设备回放流播放（默认 UDP 流模式）
     *
     * @param deviceId 设备ID，不能为空
     * @param sdpIp SDP中的收流IP地址
     * @param mediaPort 收流媒体端口
     * @param startTime 回放开始时间（格式：yyyy-MM-ddTHH:mm:ss）
     * @param endTime 回放结束时间（格式：yyyy-MM-ddTHH:mm:ss）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> invitePlayBack(String deviceId, String sdpIp, Integer mediaPort,
        String startTime, String endTime) {
        return invitePlayBack(deviceId, sdpIp, mediaPort, StreamModeEnum.UDP, startTime, endTime);
    }

    /**
     * 控制设备回放播放
     * <p>
     * 向设备发送INFO请求，控制回放的播放状态（继续播放、暂停等）。
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
            () -> serverCommandSender.deviceInvitePlayBackControl(deviceId, playAction),
            deviceId, playAction);
    }

    /**
     * 发送ACK响应（不带callId，框架自动匹配事务）
     *
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> sendAck(String deviceId) {
        validateDeviceId(deviceId, "发送ACK响应时设备ID不能为空");

        return executeCommand("sendAck", deviceId,
            () -> serverCommandSender.deviceAck(deviceId),
            deviceId);
    }

    /**
     * 发送ACK响应（指定callId，用于 INVITE 三向握手最后一步）
     *
     * @param deviceId 设备ID，不能为空
     * @param callId INVITE 阶段的 Call-ID
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> sendAck(String deviceId, String callId) {
        validateDeviceId(deviceId, "发送ACK响应时设备ID不能为空");
        validateNotNull(callId, "呼叫ID不能为空");

        return executeCommand("sendAck", deviceId,
            () -> serverCommandSender.deviceAck(deviceId, callId),
            deviceId, callId);
    }

    /**
     * 发送BYE请求结束会话（dialog-aware，按 callId）
     * <p>
     * 1.8.0 起 BYE 不再需要 deviceId，信息全部从 dialog 取回。
     * </p>
     *
     * @param callId INVITE 阶段记录的 Call-ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当 callId 为空时抛出
     */
    public ResultDTO<Void> sendBye(String callId) {
        validateNotNull(callId, "呼叫ID不能为空");

        return executeCommand("sendBye",
            () -> serverCommandSender.deviceBye(callId),
            callId);
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
            () -> serverCommandSender.deviceBroadcast(deviceId),
            deviceId);
    }

    // ==================== 回放控制便捷方法 ====================

    /**
     * 继续回放
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> playBack(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.PLAY_NOW);
    }

    /**
     * 暂停回放
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> pauseBack(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.PLAY_RESUME);
    }
}
