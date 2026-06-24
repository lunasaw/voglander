package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import javax.sip.RequestEvent;

import gov.nist.javax.sip.message.SIPRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import io.github.lunasaw.sip.common.utils.SipUtils;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabChannelHolder;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab.LabSessionHolder;
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

    /**
     * Lab 会话 holder 软注入：该 supplier 全局 @Primary、非 lab 条件化，
     * lab 关闭时 {@link LabSessionHolder} Bean 不存在，故用 ObjectProvider 软注入。
     * {@code getIfAvailable()} 在 Bean 缺失时返回 null，天然兼容 lab 关闭。
     */
    @Autowired(required = false)
    private ObjectProvider<LabSessionHolder> labSessionHolderProvider;

    /**
     * Lab 模拟通道 holder 软注入。与 {@link #labSessionHolderProvider} 同理：
     * lab 关闭时 {@link LabChannelHolder} Bean 不存在，{@code getIfAvailable()} 返回 null。
     * 供 {@link #checkDevice} 判定入站 INVITE 的 channelId 是否为本设备所属通道。
     */
    @Autowired(required = false)
    private ObjectProvider<LabChannelHolder> labChannelHolderProvider;

    private FromDevice                   clientFromDevice;

    @Override
    public FromDevice getClientFromDevice() {
        // Lab 模式：优先使用 LabSessionHolder 的 clientId 覆盖
        LabSessionHolder.Snapshot snapshot = currentLabSnapshot();
        String effectiveClientId = (snapshot != null && snapshot.getClientId() != null)
            ? snapshot.getClientId()
            : (clientProperties != null ? clientProperties.getClientId() : null);

        // 如果 clientId 变化了，重新创建 FromDevice
        if (clientFromDevice != null && !clientFromDevice.getUserId().equals(effectiveClientId)) {
            log.info("Lab clientId 变化，重新创建 FromDevice: {} -> {}", clientFromDevice.getUserId(), effectiveClientId);
            clientFromDevice = null;
        }

        if (clientFromDevice == null) {
            if (clientProperties == null) {
                log.error("VoglanderSipClientProperties未注入，无法创建客户端设备");
                return null;
            }

            clientFromDevice = new FromDevice();
            clientFromDevice.setUserId(effectiveClientId);

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
            // Lab 模式：优先使用 snapshot 的密码覆盖
            String effectivePassword = (snapshot != null && snapshot.getClientPassword() != null)
                ? snapshot.getClientPassword()
                : (clientProperties != null ? clientProperties.getPassword() : null);
            clientFromDevice.setPassword(effectivePassword);

            log.info("创建客户端FromDevice: deviceId={}, ip={}, port={}, realm={}",
                effectiveClientId, ip, port, clientProperties.getRealm());
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

    /**
     * 判定入站请求（INVITE/MESSAGE 等）是否发给本客户端 UA。
     * <p>
     * 框架接口默认实现仅比对 {@code To 头 userId == 本端 clientId}，只认寻址到「设备」的请求。
     * 但 GB28181 点播寻址到「通道」：Request-URI/To 的 userId 为 channelId 而非 clientId，
     * 默认实现会判否 → 框架 {@code InviteRequestProcessor} 首行 {@code if(!checkDevice) return} 静默丢弃，
     * 客户端既不回 200 OK 也不抛 {@code ClientInviteEvent}，平台 INVITE 必然超时 408。
     * </p>
     * <p>
     * 故扩展判定：To 头 userId 等于本端 clientId（寻址到设备本身），<b>或</b>是本设备拥有的某个通道。
     * 通道归属委托 {@link LabChannelHolder#ownsChannel}（与目录回包同一编码规则单点维护，避免生成/判定漂移）。
     * lab 关闭（holder 不存在）时仅认 clientId；真实设备接入应由各自 supplier 按其真实通道清单实现，不走此 Lab 兜底。
     * </p>
     */
    @Override
    public boolean checkDevice(RequestEvent evt) {
        try {
            SIPRequest request = (SIPRequest)evt.getRequest();
            String toUserId = SipUtils.getUserIdFromToHeader(request);
            FromDevice from = getClientFromDevice();
            String clientId = from != null ? from.getUserId() : null;

            log.warn("=== DEBUG: VoglanderClientDeviceSupplier.checkDevice() toUserId={}, clientId={}, from={}",
                toUserId, clientId, from);

            if (clientId == null || toUserId == null) {
                log.warn("=== DEBUG: checkDevice返回false - clientId或toUserId为null");
                return false;
            }
            // 寻址到设备本身
            if (toUserId.equals(clientId)) {
                log.warn("=== DEBUG: checkDevice返回true - toUserId匹配clientId");
                return true;
            }
            // 寻址到本设备的某个通道（GB28181 点播 To=channelId）
            LabChannelHolder holder = labChannelHolderProvider != null ? labChannelHolderProvider.getIfAvailable() : null;
            boolean ownsChannel = holder != null && holder.ownsChannel(clientId, toUserId);
            log.warn("=== DEBUG: checkDevice通道判定 - holder={}, ownsChannel={}", holder != null, ownsChannel);
            return ownsChannel;
        } catch (Exception e) {
            log.error("客户端设备校验失败", e);
            return false;
        }
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
     * Lab 401 鉴权重发兜底：当 REGISTER 响应 To 头指向的目标设备查不到 DB 时，
     * 命中以下两种之一即用 properties / holder 构造目标 ToDevice，使二次 REGISTER 能拿到目标地址：
     * <ol>
     *   <li>deviceId == 本地 serverId（自环，目标=本进程平台 5060）；</li>
     *   <li>deviceId == LabSessionHolder 当前快照的外部 serverId（注册到外部平台）。</li>
     * </ol>
     * 普通外部设备查不到仍返回 {@code null}，不污染常规路径。
     *
     * @param deviceId 目标设备 ID（来自 REGISTER 响应 To 头）
     * @return 目标 ToDevice；不命中返回 {@code null}
     */
    private ToDevice buildLabServerDevice(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        // 分支二（外部目标）：lab 开启且 holder 有外部 serverId 快照命中
        LabSessionHolder.Snapshot snapshot = currentLabSnapshot();
        if (snapshot != null && deviceId.equals(snapshot.getServerId())) {
            return buildToFromSnapshot(snapshot);
        }
        // 分支一（自环）：目标=本进程平台自身
        if (serverProperties != null && deviceId.equals(serverProperties.getServerId())) {
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
        return null;
    }

    /** lab 关闭时 provider 为 null（Bean 不存在）→ 返回 null，等价无外部覆盖。 */
    private LabSessionHolder.Snapshot currentLabSnapshot() {
        if (labSessionHolderProvider == null) {
            return null;
        }
        LabSessionHolder holder = labSessionHolderProvider.getIfAvailable();
        return holder != null ? holder.current() : null;
    }

    /** 用 holder 快照构造外部目标 ToDevice；serverId 必非空（调用前已校验命中）。 */
    private ToDevice buildToFromSnapshot(LabSessionHolder.Snapshot s) {
        String ip = s.getServerIp() != null ? s.getServerIp()
            : (serverProperties != null ? serverProperties.getIp() : "127.0.0.1");
        int port = s.getServerPort() != null ? s.getServerPort()
            : (serverProperties != null ? serverProperties.getPort() : 5060);
        String domain = s.getServerDomain() != null ? s.getServerDomain() : s.getServerId();
        String transport = s.getTransport() != null ? s.getTransport()
            : (clientProperties != null ? clientProperties.getTransport() : null);

        ToDevice toDevice = new ToDevice();
        toDevice.setUserId(s.getServerId());
        toDevice.setIp(ip);
        toDevice.setPort(port);
        toDevice.setHostAddress(ip + ":" + port);
        toDevice.setRealm(extractRealm(domain));
        toDevice.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
        toDevice.setCharset("UTF-8");
        log.info("Lab 外部目标解析: serverId={}, host={}:{}, transport={}",
            s.getServerId(), ip, port, toDevice.getTransport());
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