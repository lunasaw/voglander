package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 协议验证台客户端 SIP 操作（Lab 模式专用）。
 * <p>
 * 与级联 {@code VoglanderClientStatusCommand} 等不同：
 * Lab 的目标是本进程内的 SIP 服务端（5060），而非从 DB 查出的外部设备；
 * 故 {@code to} 由 {@link VoglanderSipServerProperties} 构造，不走 {@code clientDeviceSupplier.getToDevice()} DB 路径。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabSipClient {

    private final VoglanderSipClientProperties clientProps;
    private final VoglanderSipServerProperties serverProps;

    /**
     * Lab 客户端 digest 密码。独立于 sip.client.password，便于联调单独覆盖；
     * 留空时回退 {@link VoglanderSipClientProperties#getPassword()}。
     */
    @Value("${voglander.protocol-lab.device-password:}")
    private String                             labDevicePassword;

    /** Lab 客户端身份（5061 → 平台 5060）。 */
    public FromDevice buildFrom() {
        FromDevice from = new FromDevice();
        from.setUserId(clientProps.getClientId());
        from.setIp(clientProps.getDomain());
        from.setPort(clientProps.getPort());
        from.setRealm(clientProps.getRealm());
        from.setPassword(StringUtils.isNotBlank(labDevicePassword)
            ? labDevicePassword : clientProps.getPassword());
        return from;
    }

    /** Lab 目标：本进程平台（5060）。 */
    public ToDevice buildTo() {
        ToDevice to = new ToDevice();
        to.setUserId(serverProps.getServerId());
        to.setIp(serverProps.getIp());
        to.setPort(serverProps.getPort());
        to.setRealm(extractRealm(serverProps.getDomain()));
        to.setTransport("TCP".equalsIgnoreCase(clientProps.getTransport()) ? "TCP" : "UDP");
        to.setCharset("UTF-8");
        return to;
    }

    /** 发送 REGISTER（expires > 0）或注销（expires = 0）。 */
    public String register(int expires) {
        log.info("Lab REGISTER → server::{}, expires={}", serverProps.getIp(), serverProps.getPort(), expires);
        return ClientCommandSender.sendRegisterCommand(buildFrom(), buildTo(), expires);
    }

    /** 发送单次心跳。 */
    public String keepalive() {
        log.info("Lab KEEPALIVE → server:{}:{}", serverProps.getIp(), serverProps.getPort());
        return ClientCommandSender.sendKeepaliveCommand(buildFrom(), buildTo(), "OK");
    }

    /**
     * 主动上报目录（生成 channelCount 条模拟通道）。
     */
    public String pushCatalog(int channelCount, String catalogName) {
        List<DeviceItem> items = new ArrayList<>(channelCount);
        for (int i = 1; i <= channelCount; i++) {
            DeviceItem item = new DeviceItem();
            item.setDeviceId(clientProps.getClientId() + String.format("%02d", i));
            item.setName(catalogName + "-ch" + i);
            item.setStatus("ON");
            item.setParental(0);
            item.setRegisterWay(1);
            item.setSafetyWay(0);
            items.add(item);
        }
        DeviceResponse resp = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), "0", clientProps.getClientId());
        resp.setSumNum(channelCount);
        resp.setDeviceItemList(items);
        log.info("Lab CATALOG → server, channelCount={}", channelCount);
        return ClientCommandSender.sendCatalogCommand(buildFrom(), buildTo(), resp);
    }

    /** 主动上报设备信息。 */
    public String pushDeviceInfo(String manufacturer, String model, String firmware) {
        DeviceInfo info = new DeviceInfo(CmdTypeEnum.DEVICE_INFO.getType(), "0", clientProps.getClientId());
        info.setManufacturer(manufacturer);
        info.setModel(model);
        info.setFirmware(firmware);
        log.info("Lab DEVICE_INFO → server, manufacturer={}", manufacturer);
        return ClientCommandSender.sendDeviceInfoCommand(buildFrom(), buildTo(), info);
    }

    /** 主动上报告警。 */
    public String pushAlarm(int alarmType, int priority, String channelId) {
        DeviceAlarm alarm = new DeviceAlarm();
        alarm.setDeviceId(clientProps.getClientId());
        alarm.setAlarmPriority(String.valueOf(priority));
        alarm.setAlarmMethod(String.valueOf(alarmType));
        alarm.setAlarmType(String.valueOf(alarmType));
        log.info("Lab ALARM → server, alarmType={}", alarmType);
        return ClientCommandSender.sendAlarmCommand(buildFrom(), buildTo(), alarm);
    }

    private String extractRealm(String id) {
        return (id != null && id.length() >= 8) ? id.substring(0, 8) : "34020000";
    }
}
