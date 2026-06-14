package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.command.Gb28181CommandType;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端设备配置指令实现类
 *
 * <h3>sip-gateway 1.8.0 envelope 改造（2026-06-01）</h3>
 * <ul>
 * <li>{@code gb28181.Config.BasicParam}: payload {@code {name, expiration, heartBeatInterval, heartBeatCount}} 全 String</li>
 * <li>{@code gb28181.Config.ConfigDownload}: payload {@code {configType: String}}</li>
 * <li>{@code gb28181.Control.Reboot}: payload 为空</li>
 * </ul>
 *
 * @author luna
 * @since 2025/8/2
 * @version 2.0
 */
@Component
public class VoglanderServerConfigCommand extends AbstractVoglanderServerCommand {


    /**
     * 配置设备基本参数。
     */
    public ResultDTO<Void> configDevice(String deviceId, String name, String expiration,
        String heartBeatInterval, String heartBeatCount) {
        validateDeviceId(deviceId, "配置设备时设备ID不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("expiration", expiration);
        payload.put("heartBeatInterval", heartBeatInterval);
        payload.put("heartBeatCount", heartBeatCount);
        return dispatchEnvelope(Gb28181CommandType.CONFIG_BASIC_PARAM.type(), deviceId, payload);
    }

    /**
     * 配置设备基本参数（默认值）。
     */
    public ResultDTO<Void> configDevice(String deviceId, String name) {
        validateNotNull(name, "设备名称不能为空");
        return configDevice(deviceId, name, "3600", "60", "3");
    }

    /**
     * 下载（查询）设备配置。
     *
     * @param configType BasicParam / VideoParamOpt / AudioParamOpt 等
     */
    public ResultDTO<Void> downloadDeviceConfig(String deviceId, String configType) {
        validateDeviceId(deviceId, "下载设备配置时设备ID不能为空");
        validateNotNull(configType, "配置类型不能为空");

        Map<String, Object> payload = new HashMap<>();
        payload.put("configType", configType);
        return dispatchEnvelope(Gb28181CommandType.CONFIG_DOWNLOAD.type(), deviceId, payload);
    }

    /**
     * 重启设备。
     */
    public ResultDTO<Void> rebootDevice(String deviceId) {
        validateDeviceId(deviceId, "重启设备时设备ID不能为空");
        return dispatchEnvelope(Gb28181CommandType.CONTROL_REBOOT.type(), deviceId, Collections.emptyMap());
    }

    // ==================== 预定义配置类型的便捷方法 ====================

    public ResultDTO<Void> downloadBasicConfig(String deviceId) {
        return downloadDeviceConfig(deviceId, "BasicParam");
    }

    public ResultDTO<Void> downloadVideoConfig(String deviceId) {
        return downloadDeviceConfig(deviceId, "VideoParamOpt");
    }

    public ResultDTO<Void> downloadAudioConfig(String deviceId) {
        return downloadDeviceConfig(deviceId, "AudioParamOpt");
    }

    // ==================== 批量配置方法 ====================

    public ResultDTO<Void> configDeviceComplete(String deviceId, String name,
        String customExpiration, String customHeartBeatInterval) {
        String expiration = customExpiration != null ? customExpiration : "3600";
        String heartBeatInterval = customHeartBeatInterval != null ? customHeartBeatInterval : "60";
        return configDevice(deviceId, name, expiration, heartBeatInterval, "3");
    }

    public ResultDTO<Void> configHighFrequencyDevice(String deviceId, String name) {
        return configDevice(deviceId, name, "3600", "30", "5");
    }

    public ResultDTO<Void> configLowFrequencyDevice(String deviceId, String name) {
        return configDevice(deviceId, name, "7200", "120", "2");
    }
}
