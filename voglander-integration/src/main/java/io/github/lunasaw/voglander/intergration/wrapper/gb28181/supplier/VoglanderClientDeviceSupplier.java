package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
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
@NoArgsConstructor
@AllArgsConstructor
@ConditionalOnMissingBean(ClientDeviceSupplier.class)
public class VoglanderClientDeviceSupplier implements ClientDeviceSupplier {

    @Autowired
    private DeviceManager                deviceManager;

    @Autowired
    private VoglanderSipClientProperties clientProperties;

    private FromDevice                   clientFromDevice;

    @Override
    public FromDevice getClientFromDevice() {
        if (clientFromDevice == null) {
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

            log.info("创建客户端FromDevice: deviceId={}, ip={}, port={}",
                clientProperties.getClientId(), ip, port);
        }
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
     * 将数据库设备对象转换为SIP设备对象
     */
    public Device convertToSipDevice(DeviceDTO deviceDTO) {
        Device device = new ToDevice();
        DeviceDTO.ExtendInfo extendInfo = deviceDTO.getExtendInfo();
        device.setUserId(deviceDTO.getDeviceId());
        device.setIp(deviceDTO.getIp());
        device.setPort(deviceDTO.getPort());
        device.setRealm(extractRealm(deviceDTO.getDeviceId()));
        device.setTransport(extendInfo.getTransport());
        device.setCharset(extendInfo.getCharset());
        device.setStreamMode(extendInfo.getStreamMode());
        // 根据需要设置其他属性
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