package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
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
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
@RequiredArgsConstructor
public class VoglanderClientDeviceSupplier implements ClientDeviceSupplier {

    private final DeviceManager                deviceManager;

    private final VoglanderSipClientProperties clientProperties;

    private FromDevice                         clientFromDevice;

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
            DeviceDO deviceDO = deviceManager.getByDeviceId(deviceId);
            if (deviceDO == null) {
                log.warn("设备不存在: {}", deviceId);
                return null;
            }
            return convertToSipDevice(deviceDO);
        } catch (Exception e) {
            log.error("获取设备失败: {}", deviceId, e);
            return null;
        }
    }

    @Override
    public ToDevice getToDevice(String deviceId) {
        try {
            DeviceDO deviceDO = deviceManager.getByDeviceId(deviceId);
            if (deviceDO == null) {
                log.warn("目标设备不存在: {}", deviceId);
                return createDefaultToDevice(deviceId);
            }
            return convertToToDevice(deviceDO);
        } catch (Exception e) {
            log.error("获取目标设备失败: {}", deviceId, e);
            return createDefaultToDevice(deviceId);
        }
    }

    /**
     * 将数据库设备对象转换为SIP设备对象
     */
    private Device convertToSipDevice(DeviceDO deviceDO) {
        Device device = new ToDevice();
        device.setUserId(deviceDO.getDeviceId());
        device.setIp(deviceDO.getIp());
        device.setPort(deviceDO.getPort());
        device.setRealm(extractRealm(deviceDO.getDeviceId()));
        // 根据需要设置其他属性
        return device;
    }

    /**
     * 将SIP设备对象转换为数据库设备对象
     */
    private DeviceDTO convertToDeviceDO(Device device) {
        DeviceDTO deviceDO = new DeviceDTO();
        deviceDO.setDeviceId(device.getUserId());
        deviceDO.setIp(device.getIp());
        deviceDO.setPort(device.getPort());
        deviceDO.setStatus(1); // 默认在线状态
        return deviceDO;
    }

    /**
     * 将数据库设备对象转换为ToDevice对象
     */
    private ToDevice convertToToDevice(DeviceDO deviceDO) {
        ToDevice toDevice = new ToDevice();
        toDevice.setUserId(deviceDO.getDeviceId());
        toDevice.setIp(deviceDO.getIp());
        toDevice.setPort(deviceDO.getPort());
        toDevice.setRealm(extractRealm(deviceDO.getDeviceId()));
        return toDevice;
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