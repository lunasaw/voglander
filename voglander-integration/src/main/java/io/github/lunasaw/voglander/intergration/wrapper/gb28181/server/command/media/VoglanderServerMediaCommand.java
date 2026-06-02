package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181服务端媒体流指令实现类
 *
 * <h3>sip-gateway 1.8.0 envelope 改造（2026-06-01）</h3>
 * <ul>
 * <li>{@code gb28181.Invite.Play}（白名单）：payload {@code {mediaIp, mediaPort:int, streamMode: name}}</li>
 * <li>{@code gb28181.Invite.Playback}（白名单）：上述 + {@code startTime, endTime}（String）</li>
 * <li>{@code gb28181.Invite.PlaybackControl}（白名单）：payload {@code {action: PlayActionEnums.name()}}</li>
 * <li>{@code gb28181.Invite.Ack}（白名单）：payload {@code {callId?: String}}</li>
 * <li>{@code gb28181.Invite.Bye}（declare 表）：payload {@code {callId: String}}, deviceId 留空</li>
 * <li>{@code gb28181.Device.Broadcast}（declare 表）：payload {} 仅 deviceId</li>
 * </ul>
 *
 * <p>
 * <strong>注意</strong>：白名单 handler 取 SDP IP 字段名为 {@code mediaIp}（不是 {@code sdpIp}）。
 * </p>
 *
 * @author luna
 * @since 2025/8/2
 * @version 2.0
 */
@Component
@Slf4j
public class VoglanderServerMediaCommand extends AbstractVoglanderServerCommand {

    private static final String TYPE_INVITE_PLAY      = "gb28181.Invite.Play";
    private static final String TYPE_INVITE_PLAYBACK  = "gb28181.Invite.Playback";
    private static final String TYPE_PLAYBACK_CONTROL = "gb28181.Invite.PlaybackControl";
    private static final String TYPE_INVITE_ACK       = "gb28181.Invite.Ack";
    private static final String TYPE_INVITE_BYE       = "gb28181.Invite.Bye";
    private static final String TYPE_BROADCAST        = "gb28181.Device.Broadcast";

    /**
     * 邀请设备实时流播放（指定流模式）。
     */
    public ResultDTO<Void> inviteRealTimePlay(String deviceId, String sdpIp, Integer mediaPort, StreamModeEnum streamMode) {
        validateDeviceId(deviceId, "邀请设备实时流播放时设备ID不能为空");
        validateNotNull(sdpIp, "SDP IP地址不能为空");
        validateNotNull(mediaPort, "媒体端口不能为空");
        validateNotNull(streamMode, "流传输模式不能为空");

        Map<String, Object> payload = buildMediaPayload(sdpIp, mediaPort, streamMode);
        return dispatchEnvelope(TYPE_INVITE_PLAY, deviceId, payload);
    }

    /**
     * 邀请设备实时流播放（默认 UDP）。
     */
    public ResultDTO<Void> inviteRealTimePlay(String deviceId, String sdpIp, Integer mediaPort) {
        return inviteRealTimePlay(deviceId, sdpIp, mediaPort, StreamModeEnum.UDP);
    }

    public ResultDTO<Void> inviteRealTimePlayUdp(String deviceId, String sdpIp, Integer mediaPort) {
        return inviteRealTimePlay(deviceId, sdpIp, mediaPort, StreamModeEnum.UDP);
    }

    public ResultDTO<Void> inviteRealTimePlayTcp(String deviceId, String sdpIp, Integer mediaPort) {
        return inviteRealTimePlay(deviceId, sdpIp, mediaPort, StreamModeEnum.TCP_ACTIVE);
    }

    /**
     * 邀请设备回放流播放（指定流模式）。
     */
    public ResultDTO<Void> invitePlayBack(String deviceId, String sdpIp, Integer mediaPort,
        StreamModeEnum streamMode, String startTime, String endTime) {
        validateDeviceId(deviceId, "邀请设备回放流播放时设备ID不能为空");
        validateNotNull(sdpIp, "SDP IP地址不能为空");
        validateNotNull(mediaPort, "媒体端口不能为空");
        validateNotNull(streamMode, "流传输模式不能为空");
        validateNotNull(startTime, "回放开始时间不能为空");
        validateNotNull(endTime, "回放结束时间不能为空");

        Map<String, Object> payload = buildMediaPayload(sdpIp, mediaPort, streamMode);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        return dispatchEnvelope(TYPE_INVITE_PLAYBACK, deviceId, payload);
    }

    public ResultDTO<Void> invitePlayBack(String deviceId, String sdpIp, Integer mediaPort,
        String startTime, String endTime) {
        return invitePlayBack(deviceId, sdpIp, mediaPort, StreamModeEnum.UDP, startTime, endTime);
    }

    /**
     * 控制设备回放（继续/暂停/Seek 等）。
     */
    public ResultDTO<Void> controlPlayBack(String deviceId, PlayActionEnums playAction) {
        validateDeviceId(deviceId, "控制设备回放播放时设备ID不能为空");
        validateNotNull(playAction, "播放操作不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", playAction.name());
        return dispatchEnvelope(TYPE_PLAYBACK_CONTROL, deviceId, payload);
    }

    /**
     * 发送 ACK（无 callId，框架自动匹配事务）。
     */
    public ResultDTO<Void> sendAck(String deviceId) {
        validateDeviceId(deviceId, "发送ACK响应时设备ID不能为空");
        return dispatchEnvelope(TYPE_INVITE_ACK, deviceId, Collections.emptyMap());
    }

    /**
     * 发送 ACK（指定 callId）。
     */
    public ResultDTO<Void> sendAck(String deviceId, String callId) {
        validateDeviceId(deviceId, "发送ACK响应时设备ID不能为空");
        validateNotNull(callId, "呼叫ID不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", callId);
        return dispatchEnvelope(TYPE_INVITE_ACK, deviceId, payload);
    }

    /**
     * 发送 BYE（dialog-aware）。
     * <p>
     * declare 表 {@code gb28181.Invite.Bye} 的 spec 为 {@code arg("callId")}，
     * 即从 payload 取 callId，envelope 顶层 deviceId 留空。
     * </p>
     */
    public ResultDTO<Void> sendBye(String callId) {
        validateNotNull(callId, "呼叫ID不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", callId);
        return dispatchEnvelope(TYPE_INVITE_BYE, null, payload);
    }

    /**
     * 发送设备广播。
     */
    public ResultDTO<Void> sendBroadcast(String deviceId) {
        validateDeviceId(deviceId, "发送设备广播时设备ID不能为空");
        return dispatchEnvelope(TYPE_BROADCAST, deviceId, Collections.emptyMap());
    }

    // ==================== 回放控制便捷方法 ====================

    public ResultDTO<Void> playBack(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.PLAY_NOW);
    }

    public ResultDTO<Void> pauseBack(String deviceId) {
        return controlPlayBack(deviceId, PlayActionEnums.PLAY_RESUME);
    }

    private static Map<String, Object> buildMediaPayload(String sdpIp, Integer mediaPort, StreamModeEnum streamMode) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("mediaIp", sdpIp);
        payload.put("mediaPort", mediaPort);
        payload.put("streamMode", streamMode.name());
        return payload;
    }
}
