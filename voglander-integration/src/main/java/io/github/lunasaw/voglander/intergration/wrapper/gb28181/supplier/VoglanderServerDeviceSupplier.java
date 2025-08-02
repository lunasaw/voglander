package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import javax.sip.RequestEvent;

import org.springframework.stereotype.Component;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ServerDeviceSupplier;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Voglander服务端设备供应器
 * 与数据库集成的ServerDeviceSupplier实现
 * 
 * @author luna
 * @since 2025/8/2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoglanderServerDeviceSupplier implements ServerDeviceSupplier {

    private final DeviceManager                deviceManager;
    private final VoglanderSipServerProperties serverProperties;

    private FromDevice                         serverFromDevice;

    @Override
    public FromDevice getServerFromDevice() {
        if (serverFromDevice == null) {
            serverFromDevice = new FromDevice();
            serverFromDevice.setUserId(serverProperties.getServerId());
            serverFromDevice.setIp(serverProperties.getIp());
            serverFromDevice.setPort(serverProperties.getPort());
            serverFromDevice.setRealm(extractRealm(serverProperties.getDomain()));

            log.info("创建服务端FromDevice: serverId={}, ip={}, port={}",
                serverProperties.getServerId(), serverProperties.getIp(), serverProperties.getPort());
        }
        return serverFromDevice;
    }

    @Override
    public void setServerFromDevice(FromDevice fromDevice) {
        this.serverFromDevice = fromDevice;
        log.debug("设置服务端FromDevice: {}", fromDevice);
    }

    @Override
    public boolean checkDevice(RequestEvent evt) {
        try {
            // 从请求事件中提取设备信息进行验证
            // 这里可以根据实际需求实现设备验证逻辑
            String deviceId = extractDeviceIdFromRequest(evt);
            if (deviceId == null) {
                log.warn("无法从请求中提取设备ID");
                return false;
            }

            DeviceDO deviceDO = deviceManager.getByDeviceId(deviceId);
            if (deviceDO == null) {
                log.warn("设备不存在或未注册: {}", deviceId);
                return false;
            }

            // 检查设备状态
            if (deviceDO.getStatus() != 1) {
                log.warn("设备状态异常: deviceId={}, status={}", deviceId, deviceDO.getStatus());
                return false;
            }

            log.debug("设备验证通过: {}", deviceId);
            return true;
        } catch (Exception e) {
            log.error("设备验证失败", e);
            return false;
        }
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
     * 从SIP请求事件中提取设备ID
     * TODO: 根据实际的SIP消息格式实现
     */
    private String extractDeviceIdFromRequest(RequestEvent evt) {
        try {
            // 这里需要根据实际的SIP消息结构来提取设备ID
            // 通常从From header或Contact header中获取
            return null; // TODO: 实现具体的提取逻辑
        } catch (Exception e) {
            log.error("提取设备ID失败", e);
            return null;
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
    private DeviceDO convertToDeviceDO(Device device) {
        DeviceDO deviceDO = new DeviceDO();
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
        toDevice.setIp(serverProperties.getIp());
        toDevice.setPort(serverProperties.getPort());
        toDevice.setRealm(extractRealm(deviceId));
        return toDevice;
    }

    /**
     * 从设备ID或域中提取realm
     */
    private String extractRealm(String deviceId) {
        if (deviceId != null && deviceId.length() >= 8) {
            return deviceId.substring(0, 8);
        }
        return "34020000";
    }
}