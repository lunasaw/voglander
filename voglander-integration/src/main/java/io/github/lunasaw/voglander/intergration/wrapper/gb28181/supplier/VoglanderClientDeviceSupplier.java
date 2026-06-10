package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Voglander客户端设备供应器
 * 与数据库集成的ClientDeviceSupplier实现
 * 
 * @author luna
 * @since 2025/8/2
 */
@Slf4j
@Component
@Primary
@NoArgsConstructor
@AllArgsConstructor
public class VoglanderClientDeviceSupplier implements ClientDeviceSupplier {

    @Autowired
    private DeviceManager                deviceManager;

    @Autowired
    private VoglanderSipClientProperties clientProperties;

    @Autowired
    private VoglanderSipServerProperties serverProperties;

    private FromDevice                   clientFromDevice;

    @Override
    public FromDevice getClientFromDevice() {
        if (clientFromDevice == null) {
            if (clientProperties == null) {
                log.error("VoglanderSipClientProperties未注入，无法创建客户端设备");
                return null;
            }

            clientFromDevice = new FromDevice();
            clientFromDevice.setUserId(clientProperties.getClientId());

            // 确保IP不为空
            String ip = clientProperties.getDomain();
            if (ip == null || ip.trim().isEmpty()) {
                ip = "127.0.0.1";
                log.warn("SIP客户端IP为空，使用默认值: {}", ip);
            }
            clientFromDevice.setIp(ip);

            // 确保端口有效
            int port = clientProperties.getPort();
            if (port <= 0 || port > 65535) {
                port = 5061;
                log.warn("SIP客户端端口无效，使用默认值: {}", port);
            }
            clientFromDevice.setPort(port);

            clientFromDevice.setRealm(clientProperties.getRealm());
            clientFromDevice.setPassword(clientProperties.getPassword());

            log.info("创建客户端FromDevice: deviceId={}, ip={}, port={}, realm={}",
                clientProperties.getClientId(), ip, port, clientProperties.getRealm());
        }

        log.debug("返回客户端FromDevice: userId={}, ip={}, port={}",
            clientFromDevice.getUserId(), clientFromDevice.getIp(), clientFromDevice.getPort());
        return clientFromDevice;
    }

    @Override
    public void setClientFromDevice(FromDevice fromDevice) {
        this.clientFromDevice = fromDevice;
        log.debug("设置客户端FromDevice: {}", fromDevice);
    }

    @Override
    public Device getDevice(String deviceId) {
        try {
            DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId(deviceId);
            if (deviceDTO == null) {
                ToDevice labServer = buildLabServerDevice(deviceId);
                if (labServer != null) {
                    return labServer;
                }
                log.warn("设备不存在: {}", deviceId);
                return null;
            }
            return convertToSipDevice(deviceDTO);
        } catch (Exception e) {
            log.error("获取设备失败: {}", deviceId, e);
            return null;
        }
    }

    @Override
    public ToDevice getToDevice(String deviceId) {
        try {
            DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId(deviceId);
            if (deviceDTO == null) {
                ToDevice labServer = buildLabServerDevice(deviceId);
                if (labServer != null) {
                    return labServer;
                }
                log.warn("设备不存在: {}", deviceId);
                return null;
            }
            Device device = convertToSipDevice(deviceDTO);
            return getToDevice(device);
        } catch (Exception e) {
            log.error("获取目标设备失败: {}", deviceId, e);
            return createDefaultToDevice(deviceId);
        }
    }

    /**
     * Lab 自环兜底：当请求的目标设备恰为本进程 SIP 平台自身（serverId）时，
     * 平台不会把自己注册进 DB，故 DB 必然查不到。此时从 {@link VoglanderSipServerProperties}
     * 构造目标 ToDevice，使客户端 401 鉴权重发能拿到目标地址（127.0.0.1:5060），
     * 否则握手断在 toDevice=null。
     * <p>
     * 仅命中"目标=平台自身"这一种情况；普通外部设备查不到仍返回 {@code null}，不污染常规路径。
     * 与 {@code LabSipClient.buildTo()} 等价。
     * </p>
     *
     * @param deviceId 目标设备 ID（来自 REGISTER 响应 To 头）
     * @return 平台自身的 ToDevice；非平台 ID 返回 {@code null}
     */
    private ToDevice buildLabServerDevice(String deviceId) {
        if (serverProperties == null || deviceId == null
            || !deviceId.equals(serverProperties.getServerId())) {
            return null;
        }
        ToDevice toDevice = new ToDevice();
        toDevice.setUserId(serverProperties.getServerId());
        toDevice.setIp(serverProperties.getIp());
        toDevice.setPort(serverProperties.getPort());
        toDevice.setHostAddress(serverProperties.getIp() + ":" + serverProperties.getPort());
        toDevice.setRealm(extractRealm(serverProperties.getDomain()));
        String transport = clientProperties != null ? clientProperties.getTransport() : null;
        toDevice.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
        toDevice.setCharset("UTF-8");
        log.info("Lab 自环目标解析为本进程平台: serverId={}, host={}:{}, transport={}",
            serverProperties.getServerId(), serverProperties.getIp(), serverProperties.getPort(), toDevice.getTransport());
        return toDevice;
    }

    /**
     * 将数据库设备对象转换为SIP设备对象
     */
    public Device convertToSipDevice(DeviceDTO deviceDTO) {
        Device device = new ToDevice();
        DeviceDTO.ExtendInfo extendInfo = deviceDTO.getExtendInfo();

        int port = deviceDTO.getPort() != null ? deviceDTO.getPort() : 5060;
        device.setUserId(deviceDTO.getDeviceId());
        device.setIp(deviceDTO.getIp());
        device.setPort(port);
        if (deviceDTO.getIp() != null) {
            device.setHostAddress(deviceDTO.getIp() + ":" + port);
        }
        device.setRealm(extractRealm(deviceDTO.getDeviceId()));

        String transport = extendInfo != null ? extendInfo.getTransport() : null;
        device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

        device.setCharset(extendInfo != null && extendInfo.getCharset() != null ? extendInfo.getCharset() : "UTF-8");
        device.setPassword(extendInfo != null ? extendInfo.getPassword() : null);

        String rawStreamMode = extendInfo != null ? extendInfo.getStreamMode() : null;
        String streamMode = rawStreamMode != null ? rawStreamMode.replace("_", "-") : StreamModeEnum.UDP.getType();
        device.setStreamMode(StreamModeEnum.isValid(streamMode) ? streamMode : StreamModeEnum.UDP.getType());

        return device;
    }

    /**
     * 创建默认的ToDevice对象
     */
    private ToDevice createDefaultToDevice(String deviceId) {
        ToDevice toDevice = new ToDevice();
        toDevice.setUserId(deviceId != null ? deviceId : "unknown");
        toDevice.setIp("127.0.0.1");
        toDevice.setPort(5060);
        toDevice.setRealm(extractRealm(deviceId));
        return toDevice;
    }

    /**
     * 从设备ID中提取realm
     */
    private String extractRealm(String deviceId) {
        if (deviceId != null && deviceId.length() >= 8) {
            return deviceId.substring(0, 8);
        }
        return "34020000";
    }
}