package io.github.lunasaw.voglander.service.command.impl;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import io.github.lunasaw.gb28181.common.entity.control.instruction.enums.PTZControlEnum;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePlayReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePlaybackReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePtzReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.alarm.VoglanderServerAlarmCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config.VoglanderServerConfigCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device.VoglanderServerDeviceCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.media.VoglanderServerMediaCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.ptz.VoglanderServerPtzCommand;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.record.VoglanderServerRecordCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * GB28181 设备命令服务门面
 * Phase 5-S3: 修复 GbDeviceCommandService bean 缺失缺陷，委托 6 个底层命令 bean
 *
 * @author luna
 */
@Slf4j
@Service("GbDeviceCommandService")
public class GbDeviceCommandService implements DeviceCommandService {

    @Autowired
    private VoglanderServerDeviceCommand deviceCommand;
    @Autowired
    private VoglanderServerPtzCommand    ptzCommand;
    @Autowired
    private VoglanderServerMediaCommand  mediaCommand;
    @Autowired
    private VoglanderServerConfigCommand configCommand;
    @Autowired
    @SuppressWarnings("unused")
    private VoglanderServerAlarmCommand  alarmCommand;
    @Autowired
    @SuppressWarnings("unused")
    private VoglanderServerRecordCommand recordCommand;

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
        PTZControlEnum control = PTZControlEnum.valueOf(req.getControl());
        return ptzCommand.controlDevicePtz(req.getDeviceId(), control, req.getSpeed());
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
}
