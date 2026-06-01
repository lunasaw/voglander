package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.device;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端设备查询指令实现类
 *
 * <h3>sip-gateway 1.8.0 envelope 改造（2026-06-01）</h3>
 * <p>
 * 5 个查询命令统一经 envelope 通道下发，type 与 payload schema 严格按
 * {@code Gb28181CommandSpecs.declare()} 对齐。
 * </p>
 *
 * <ul>
 * <li>{@code gb28181.Query.DeviceInfo} - payload {}</li>
 * <li>{@code gb28181.Query.DeviceStatus} - payload {}</li>
 * <li>{@code gb28181.Query.Catalog} - payload {}</li>
 * <li>{@code gb28181.Query.PresetQuery} - payload {}</li>
 * <li>{@code gb28181.Query.MobilePosition} - payload {interval: String}</li>
 * </ul>
 *
 * @author luna
 * @since 2025/8/2
 * @version 2.0
 */
@Component
public class VoglanderServerDeviceCommand extends AbstractVoglanderServerCommand {

    private static final String TYPE_DEVICE_INFO     = "gb28181.Query.DeviceInfo";
    private static final String TYPE_DEVICE_STATUS   = "gb28181.Query.DeviceStatus";
    private static final String TYPE_CATALOG         = "gb28181.Query.Catalog";
    private static final String TYPE_PRESET_QUERY    = "gb28181.Query.PresetQuery";
    private static final String TYPE_MOBILE_POSITION = "gb28181.Query.MobilePosition";

    /**
     * 默认 MobilePosition 查询间隔（秒）。
     */
    private static final String DEFAULT_INTERVAL     = "5";

    /**
     * 查询设备信息。
     */
    public ResultDTO<Void> queryDeviceInfo(String deviceId) {
        validateDeviceId(deviceId, "查询设备信息时设备ID不能为空");
        return dispatchEnvelope(TYPE_DEVICE_INFO, deviceId, Collections.emptyMap());
    }

    /**
     * 查询设备状态。
     */
    public ResultDTO<Void> queryDeviceStatus(String deviceId) {
        validateDeviceId(deviceId, "查询设备状态时设备ID不能为空");
        return dispatchEnvelope(TYPE_DEVICE_STATUS, deviceId, Collections.emptyMap());
    }

    /**
     * 查询设备目录。
     */
    public ResultDTO<Void> queryDeviceCatalog(String deviceId) {
        validateDeviceId(deviceId, "查询设备目录时设备ID不能为空");
        return dispatchEnvelope(TYPE_CATALOG, deviceId, Collections.emptyMap());
    }

    /**
     * 查询设备预设位。
     */
    public ResultDTO<Void> queryDevicePreset(String deviceId) {
        validateDeviceId(deviceId, "查询设备预设位时设备ID不能为空");
        return dispatchEnvelope(TYPE_PRESET_QUERY, deviceId, Collections.emptyMap());
    }

    /**
     * 查询移动设备位置。
     *
     * @param interval 查询间隔（秒），可为空（使用默认 5 秒）
     */
    public ResultDTO<Void> queryDeviceMobilePosition(String deviceId, String interval) {
        validateDeviceId(deviceId, "查询移动设备位置时设备ID不能为空");

        String queryInterval = interval != null ? interval : DEFAULT_INTERVAL;
        Map<String, Object> payload = new HashMap<>();
        payload.put("interval", queryInterval);
        return dispatchEnvelope(TYPE_MOBILE_POSITION, deviceId, payload);
    }

    /**
     * 查询移动设备位置（默认间隔）。
     */
    public ResultDTO<Void> queryDeviceMobilePosition(String deviceId) {
        return queryDeviceMobilePosition(deviceId, null);
    }
}
