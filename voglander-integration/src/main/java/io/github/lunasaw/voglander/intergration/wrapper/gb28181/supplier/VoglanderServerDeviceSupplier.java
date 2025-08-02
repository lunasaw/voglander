package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.utils.SipUtils;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@NoArgsConstructor
@ConditionalOnMissingBean(ServerDeviceSupplier.class)
public class VoglanderServerDeviceSupplier implements ServerDeviceSupplier {

    @Autowired
    private DeviceManager                deviceManager;
    @Autowired
    private VoglanderSipServerProperties serverProperties;

    private FromDevice                         serverFromDevice;

    @Override
    public FromDevice getServerFromDevice() {
        if (serverFromDevice == null) {
            serverFromDevice = new FromDevice();
            serverFromDevice.setUserId(serverProperties.getServerId());
            serverFromDevice.setIp(serverProperties.getIp());
            serverFromDevice.setPort(serverProperties.getPort());
            serverFromDevice.setRealm(extractRealm(serverProperties.getDomain()));
            serverFromDevice.setTransport("UDP");
            serverFromDevice.setCharset("UTF-8");

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

            DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId(deviceId);
            if (deviceDTO == null) {
                log.warn("设备不存在或未注册: {}", deviceId);
                return false;
            }

            // 检查设备状态
            if (deviceDTO.getStatus() != 1) {
                log.warn("设备状态异常: deviceId={}, status={}", deviceId, deviceDTO.getStatus());
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

    @Override
    public ToDevice getToDevice(String deviceId) {
        try {
            DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId(deviceId);
            if (deviceDTO == null) {
                log.warn("设备不存在: {}", deviceId);
                return createDefaultToDevice(deviceId);
            }
            Device device = convertToSipDevice(deviceDTO);
            return getToDevice(device);
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
            SIPRequest request = (SIPRequest)evt.getRequest();
            // 在接收端看来 收到请求的时候fromHeader还是服务端的 toHeader才是自己的，这里是要查询自己的信息
            return SipUtils.getUserIdFromToHeader(request);
        } catch (Exception e) {
            log.error("提取设备ID失败", e);
            return null;
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
        device.setTransport(extendInfo != null ? extendInfo.getTransport() : "UDP");
        device.setCharset(extendInfo != null ? extendInfo.getCharset() : "UTF-8");

        // 规范化streamMode：将TCP-ACTIVE转换为TCP_ACTIVE
        String streamMode = extendInfo != null ? extendInfo.getStreamMode() : "TCP_ACTIVE";
        if ("TCP-ACTIVE".equals(streamMode)) {
            streamMode = "TCP_ACTIVE";
        }
        device.setStreamMode(streamMode);

        return device;
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
        toDevice.setTransport("UDP");
        toDevice.setCharset("UTF-8");
        log.debug("创建默认ToDevice: userId={}, ip={}, port={}", toDevice.getUserId(), toDevice.getIp(), toDevice.getPort());
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