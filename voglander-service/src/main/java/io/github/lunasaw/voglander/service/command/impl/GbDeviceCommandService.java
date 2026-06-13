package io.github.lunasaw.voglander.service.command.impl;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.gbproxy.server.enums.PlayActionEnums;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceAlarmQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceConfigReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePlayReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePlaybackReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePtzReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRecordQueryReq;
import io.github.lunasaw.voglander.client.service.device.Gb28181DeviceCommandService;
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config.VoglanderServerConfigCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.manager.MediaSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * GB28181 设备命令服务门面
 * Phase 5-S3: 修复 GbDeviceCommandService bean 缺失缺陷，委托 6 个底层命令 bean
 *
 * <p>
 * 1.0.7 S2：实现 {@link Gb28181DeviceCommandService} 子接口承载 GB 专属支链（查询补全/配置/录像控制/报警/广播），
 * 同时仍是协议无关 SPI {@link DeviceCommandService} 的 GB 实现，DeviceAgreementService 路由不受影响。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Service("GbDeviceCommandService")
public class GbDeviceCommandService implements Gb28181DeviceCommandService {

    @Autowired
    private VoglanderServerDeviceCommand deviceCommand;
    @Autowired
    private VoglanderServerPtzCommand    ptzCommand;
    @Autowired
    private VoglanderServerMediaCommand  mediaCommand;
    @Autowired
    private VoglanderServerConfigCommand configCommand;
    @Autowired
    private VoglanderServerAlarmCommand  alarmCommand;
    @Autowired
    private VoglanderServerRecordCommand recordCommand;
    @Autowired
    private MediaSessionManager          mediaSessionManager;

    /**
     * L1 本次支持的回放控制动作（暂停/继续，envelope payload 仅含 action，无需 param）。
     * {@code PLAY_RANGE}(seek)/{@code PLAY_SPEED}(倍速) 不在此集，显式不支持。
     */
    private static final Set<PlayActionEnums> PLAYBACK_L1_SUPPORTED =
        EnumSet.of(PlayActionEnums.PLAY_NOW, PlayActionEnums.PLAY_RESUME);

    /**
     * 前端 PTZ 词表 → {@link PTZControlEnum} 规范枚举映射（门面层翻译）。
     * <p>
     * 前端按 {@code UP/DOWN/LEFT/...} 发送，{@code PTZControlEnum} 常量名为 {@code TILT_UP/PAN_LEFT/...}，
     * 二者不一致，直接 {@code valueOf} 必抛 {@code No enum constant}。此表把前端约定词翻译为规范枚举。
     */
    private static final Map<String, PTZControlEnum> PTZ_VOCAB = Map.ofEntries(
        Map.entry("UP", PTZControlEnum.TILT_UP),
        Map.entry("DOWN", PTZControlEnum.TILT_DOWN),
        Map.entry("LEFT", PTZControlEnum.PAN_LEFT),
        Map.entry("RIGHT", PTZControlEnum.PAN_RIGHT),
        Map.entry("UP_LEFT", PTZControlEnum.PAN_LEFT_TILT_UP),
        Map.entry("UP_RIGHT", PTZControlEnum.PAN_RIGHT_TILT_UP),
        Map.entry("DOWN_LEFT", PTZControlEnum.PAN_LEFT_TILT_DOWN),
        Map.entry("DOWN_RIGHT", PTZControlEnum.PAN_RIGHT_TILT_DOWN),
        Map.entry("ZOOM_IN", PTZControlEnum.ZOOM_IN),
        Map.entry("ZOOM_OUT", PTZControlEnum.ZOOM_OUT),
        Map.entry("STOP", PTZControlEnum.STOP));

    /**
     * PROTOCOL-S1：本实现支持的纯协议——GB28181。GB28181_IPC/NVR 两型态都映射到此协议。
     */
    @Override
    public Set<Integer> supportProtocols() {
        return Set.of(DeviceProtocolEnum.GB28181.getType());
    }

    @Override
    public ResultDTO<Void> queryChannel(DeviceQueryReq req) {
        return deviceCommand.queryDeviceCatalog(req.getDeviceId());
    }

    @Override
    public ResultDTO<Void> queryDevice(DeviceQueryReq req) {
        return deviceCommand.queryDeviceInfo(req.getDeviceId());
    }

    @Override
    public ResultDTO<Void> queryDeviceInfo(String deviceId) {
        return deviceCommand.queryDeviceInfo(deviceId);
    }

    @Override
    public ResultDTO<Void> queryCatalog(String deviceId) {
        return deviceCommand.queryDeviceCatalog(deviceId);
    }

    @Override
    public ResultDTO<Void> ptzControl(DevicePtzReq req) {
        PTZControlEnum control = resolvePtz(req.getControl());
        return ptzCommand.controlDevicePtz(req.getDeviceId(), control, req.getSpeed());
    }

    /**
     * 解析前端 PTZ 指令：先查词表翻译，再兼容直接传规范枚举名，均不命中则抛 {@link ServiceException}。
     * <p>
     * 不可用 {@code PTZControlEnum.getByName}——其键是枚举的<b>中文 name 字段</b>（"停止"/"向左"…），
     * 无法解析 {@code STOP}/{@code TILT_UP} 这类规范名；故 fallback 用 {@code valueOf} + try-catch。
     *
     * @param control 前端指令（词表词或规范枚举名，大小写/空白无关）
     * @return 规范 PTZ 枚举
     * @throws ServiceException 指令为空或未知
     */
    private PTZControlEnum resolvePtz(String control) {
        if (control == null || control.trim().isEmpty()) {
            throw new ServiceException(ServiceExceptionEnum.PTZ_COMMAND_INVALID, "PTZ 指令为空");
        }
        String key = control.trim().toUpperCase();
        PTZControlEnum c = PTZ_VOCAB.get(key);
        if (c == null) {
            try {
                c = PTZControlEnum.valueOf(key); // 兼容直接传规范枚举名（如 "TILT_UP"）
            } catch (IllegalArgumentException ignore) {
                // 落到下方统一异常
            }
        }
        if (c == null) {
            throw new ServiceException(ServiceExceptionEnum.PTZ_COMMAND_INVALID, "未知 PTZ 指令: " + control);
        }
        return c;
    }

    @Override
    public ResultDTO<String> startPlay(DevicePlayReq req) {
        /* callId 通过 MediaSessionManager.onInviteOk 异步产生，暂返回 null */
        StreamModeEnum mode = toStreamMode(req.getStreamMode());
        ResultDTO<Void> r = mediaCommand.inviteRealTimePlay(req.getDeviceId(), req.getSdpIp(), req.getMediaPort(), mode);
        return r.isSuccess() ? ResultDTOUtils.success(null) : ResultDTOUtils.failure(r.getCode(), r.getMessage(), null);
    }

    @Override
    public ResultDTO<String> startPlayback(DevicePlaybackReq req) {
        StreamModeEnum mode = toStreamMode(req.getStreamMode());
        ResultDTO<Void> r = mediaCommand.invitePlayBack(req.getDeviceId(), req.getSdpIp(), req.getMediaPort(), mode,
            req.getStartTime(), req.getEndTime());
        return r.isSuccess() ? ResultDTOUtils.success(null) : ResultDTOUtils.failure(r.getCode(), r.getMessage(), null);
    }

    @Override
    public ResultDTO<Void> stopPlay(String callId) {
        return mediaCommand.sendBye(callId);
    }

    @Override
    public ResultDTO<Void> reboot(String deviceId) {
        return configCommand.rebootDevice(deviceId);
    }

    @Override
    public ResultDTO<Void> controlPlayback(String streamId, String action, String param) {
        if (streamId == null || streamId.trim().isEmpty()) {
            return ResultDTOUtils.failure(ServiceExceptionEnum.PARAM_ERROR.getCode(), "streamId 不能为空");
        }
        // 解析动作（L1 仅支持 PLAY_NOW/PLAY_RESUME）
        PlayActionEnums playAction;
        try {
            playAction = PlayActionEnums.valueOf(action == null ? "" : action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResultDTOUtils.failure(ServiceExceptionEnum.PLAYBACK_CONTROL_FAILED.getCode(),
                "未知回放控制动作: " + action);
        }
        if (!PLAYBACK_L1_SUPPORTED.contains(playAction)) {
            // PLAY_RANGE(seek)/PLAY_SPEED(倍速)：envelope payload 当前仅含 action，无法透传 param，本次显式不支持
            return ResultDTOUtils.failure(ServiceExceptionEnum.PLAYBACK_CONTROL_FAILED.getCode(),
                "回放 " + playAction.name() + " 暂不支持（seek/倍速待后续排期）");
        }
        // streamId 反查会话得 deviceId
        MediaSessionDTO session = mediaSessionManager.getByStreamId(streamId);
        if (session == null || session.getDeviceId() == null) {
            return ResultDTOUtils.failure(ServiceExceptionEnum.LIVE_STREAM_NOT_FOUND.getCode(),
                ServiceExceptionEnum.LIVE_STREAM_NOT_FOUND.getMessage());
        }
        return mediaCommand.controlPlayBack(session.getDeviceId(), playAction);
    }

    private StreamModeEnum toStreamMode(String mode) {
        if (mode == null) {
            return StreamModeEnum.UDP;
        }
        try {
            return StreamModeEnum.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知流模式 '{}', 回退 UDP", mode);
            return StreamModeEnum.UDP;
        }
    }

    // ================================
    // 1.0.7 S2：GB28181 专属支链（Gb28181DeviceCommandService）——纯委托底层命令 bean
    // ================================

    @Override
    public ResultDTO<Void> queryDeviceStatus(String deviceId) {
        return deviceCommand.queryDeviceStatus(deviceId);
    }

    @Override
    public ResultDTO<Void> queryPreset(String deviceId) {
        return deviceCommand.queryDevicePreset(deviceId);
    }

    @Override
    public ResultDTO<Void> queryMobilePosition(String deviceId, String interval) {
        return deviceCommand.queryDeviceMobilePosition(deviceId, interval);
    }

    @Override
    public ResultDTO<Void> downloadConfig(String deviceId, String configType) {
        return configCommand.downloadDeviceConfig(deviceId, configType);
    }

    @Override
    public ResultDTO<Void> setDeviceConfig(DeviceConfigReq req) {
        return configCommand.configDevice(req.getDeviceId(), req.getName(), req.getExpiration(),
            req.getHeartBeatInterval(), req.getHeartBeatCount());
    }

    @Override
    public ResultDTO<Void> controlRecord(String deviceId, boolean start) {
        return start ? recordCommand.startDeviceRecord(deviceId) : recordCommand.stopDeviceRecord(deviceId);
    }

    @Override
    public ResultDTO<Void> queryRecord(DeviceRecordQueryReq req) {
        long start = req.getStartTime() != null ? req.getStartTime() : 0L;
        long end = req.getEndTime() != null ? req.getEndTime() : 0L;
        return recordCommand.queryDeviceRecord(req.getDeviceId(), start, end);
    }

    @Override
    public ResultDTO<Void> queryAlarm(DeviceAlarmQueryReq req) {
        Date start = req.getStartTime() != null ? new Date(req.getStartTime()) : null;
        Date end = req.getEndTime() != null ? new Date(req.getEndTime()) : null;
        return alarmCommand.queryDeviceAlarm(req.getDeviceId(), start, end,
            req.getStartPriority(), req.getEndPriority(), req.getAlarmMethod(), req.getAlarmType());
    }

    @Override
    public ResultDTO<Void> controlAlarm(String deviceId, String alarmMethod, String alarmType) {
        return alarmCommand.controlDeviceAlarm(deviceId, alarmMethod, alarmType);
    }

    @Override
    public ResultDTO<Void> broadcast(String deviceId) {
        return mediaCommand.sendBroadcast(deviceId);
    }
}
