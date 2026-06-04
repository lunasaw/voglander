package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import javax.sip.RequestEvent;
import javax.sip.message.Response;

import gov.nist.javax.sip.message.SIPRequest;
import io.github.lunasaw.sip.common.utils.SipUtils;
import io.github.lunasaw.sip.common.utils.SipRequestUtils;
import io.github.lunasaw.sip.common.constant.Constant;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
 * <p>
 * 标注 {@link Primary}：1.8.0 的 @EnableSipServer 会激活框架默认的 DefaultServerDeviceSupplier
 * （{@code @ConditionalOnMissingBean(name=ServerDeviceSupplier FQN)}），其条件基于 bean 名而非类型，
 * 组件扫描顺序下两者可能共存，故用 @Primary 确保业务实现优先注入。
 *
 * @author luna
 * @since 2025/8/2
 */
@Slf4j
@NoArgsConstructor
@Configuration
@Primary
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

            // 设置hostAddress - 关键字段，用于SIP URI创建
            serverFromDevice.setHostAddress(serverProperties.getIp() + ":" + serverProperties.getPort());

            serverFromDevice.setRealm(extractRealm(serverProperties.getDomain()));
            serverFromDevice.setTransport("UDP");
            serverFromDevice.setCharset("UTF-8");

            // 添加缺失的关键字段
            serverFromDevice.setFromTag(SipRequestUtils.getNewFromTag());
            serverFromDevice.setAgent(Constant.AGENT);

            log.info("创建服务端FromDevice: serverId={}, hostAddress={}, ip={}, port={}, fromTag={}, agent={}",
                serverProperties.getServerId(), serverFromDevice.getHostAddress(),
                serverProperties.getIp(), serverProperties.getPort(),
                serverFromDevice.getFromTag(), serverFromDevice.getAgent());
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
     * 重写接口默认方法，正确设置ToDevice的特有字段
     */
    @Override
    public ToDevice getToDevice(Device device) {
        if (device == null) {
            return null;
        }

        ToDevice toDevice = new ToDevice();
        // 复制基础字段
        toDevice.setHostAddress(device.getHostAddress());
        toDevice.setUserId(device.getUserId());
        toDevice.setRealm(device.getRealm());
        toDevice.setTransport(device.getTransport());
        toDevice.setStreamMode(device.getStreamMode());
        toDevice.setIp(device.getIp());
        toDevice.setPort(device.getPort());
        toDevice.setPassword(device.getPassword());
        toDevice.setCharset(device.getCharset());

        // 设置ToDevice特有字段
        // 注：1.8.0 移除了 Device.localIp，本端 IP 由 SipLayer monitorIp 统一管理
        toDevice.setExpires(3600);

        // 修复关键字段：为SIP请求创建设置必需的标识字段
        toDevice.setCallId(SipRequestUtils.getNewCallId());
        // 使用fromTag生成器创建toTag
        toDevice.setToTag(SipRequestUtils.getNewFromTag());

        // 为INFO请求等特殊消息类型设置subject字段
        toDevice.setSubject("GB28181:Play");

        log.debug("创建ToDevice: userId={}, ip={}, port={}, expires={}, callId={}, toTag={}",
            toDevice.getUserId(), toDevice.getIp(), toDevice.getPort(),
            toDevice.getExpires(), toDevice.getCallId(), toDevice.getToTag());

        return toDevice;
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

        int port = deviceDTO.getPort() != null ? deviceDTO.getPort() : 5060;

        device.setUserId(deviceDTO.getDeviceId());
        device.setIp(deviceDTO.getIp());
        device.setPort(port);

        if (deviceDTO.getIp() != null) {
            device.setHostAddress(deviceDTO.getIp() + ":" + port);
        } else {
            log.warn("设备IP为null，无法创建hostAddress: deviceId={}", deviceDTO.getDeviceId());
        }

        device.setRealm(extractRealm(deviceDTO.getDeviceId()));

        // SIP transport 只允许 UDP/TCP
        String transport = extendInfo != null ? extendInfo.getTransport() : null;
        device.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");

        device.setCharset(extendInfo != null && extendInfo.getCharset() != null ? extendInfo.getCharset() : "UTF-8");
        device.setPassword(extendInfo != null ? extendInfo.getPassword() : null);

        // streamMode 规范化：数据库可能存 TCP_ACTIVE/TCP_PASSIVE（下划线），转为枚举标准格式（连字符）
        // StreamModeEnum 标准值：UDP / TCP-ACTIVE / TCP-PASSIVE
        String rawStreamMode = extendInfo != null ? extendInfo.getStreamMode() : null;
        String streamMode = rawStreamMode != null ? rawStreamMode.replace("_", "-") : StreamModeEnum.UDP.getType();
        if (!StreamModeEnum.isValid(streamMode)) {
            log.warn("无效streamMode: {}，回退为UDP", rawStreamMode);
            streamMode = StreamModeEnum.UDP.getType();
        }
        device.setStreamMode(streamMode);

        log.info("转换SIP设备: userId={}, hostAddress={}, ip={}, port={}, transport={}, streamMode={}",
            device.getUserId(), device.getHostAddress(), device.getIp(), device.getPort(), device.getTransport(), device.getStreamMode());

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

        // 设置hostAddress - 关键字段
        toDevice.setHostAddress(serverProperties.getIp() + ":" + serverProperties.getPort());

        toDevice.setRealm(extractRealm(deviceId));
        toDevice.setTransport("UDP");
        toDevice.setCharset("UTF-8");

        // 设置ToDevice特有字段
        // 注：1.8.0 移除了 Device.localIp，本端 IP 由 SipLayer monitorIp 统一管理
        toDevice.setExpires(3600);

        // 修复关键字段：为SIP请求创建设置必需的标识字段
        toDevice.setCallId(SipRequestUtils.getNewCallId());
        toDevice.setToTag(SipRequestUtils.getNewFromTag());
        toDevice.setSubject("GB28181:Play");

        log.info("创建默认ToDevice: userId={}, hostAddress={}, ip={}, port={}, callId={}, toTag={}",
            toDevice.getUserId(), toDevice.getHostAddress(), toDevice.getIp(), toDevice.getPort(),
            toDevice.getCallId(), toDevice.getToTag());
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