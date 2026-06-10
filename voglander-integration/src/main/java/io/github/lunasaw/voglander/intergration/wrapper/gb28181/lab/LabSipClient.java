package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.notify.DeviceAlarmNotify;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;
import com.luna.common.text.RandomStrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
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
    private final LabSessionHolder             labSessionHolder;
    private final LabChannelHolder             labChannelHolder;

    /**
     * Lab 客户端 digest 密码。独立于 sip.client.password，便于联调单独覆盖；
     * 留空时回退 {@link VoglanderSipClientProperties#getPassword()}。
     */
    @Value("${voglander.protocol-lab.device-password:}")
    private String                             labDevicePassword;

    /** 取非空覆盖，否则回退。 */
    private static String pick(String override, String fallback) {
        return StringUtils.isNotBlank(override) ? override : fallback;
    }

    /** Lab 客户端身份（5061 → 目标平台）。holder 覆盖身份，ip/port 恒为本机监听地址。 */
    public FromDevice buildFrom() {
        LabSessionHolder.Snapshot s = labSessionHolder.current();
        FromDevice from = new FromDevice();
        from.setUserId(pick(s != null ? s.getClientId() : null, clientProps.getClientId()));
        // ip/port 不随目标改：是本机 5061 监听绑定地址，改了收不到平台回包
        from.setIp(clientProps.getDomain());
        from.setPort(clientProps.getPort());
        from.setRealm(clientProps.getRealm());
        // 密码优先级：holder > voglander.protocol-lab.device-password > sip.client.password
        String pwd = s != null && StringUtils.isNotBlank(s.getClientPassword())
            ? s.getClientPassword()
            : (StringUtils.isNotBlank(labDevicePassword) ? labDevicePassword : clientProps.getPassword());
        from.setPassword(pwd);
        return from;
    }

    /** Lab 目标：holder 优先，回退本进程平台（5060 自环）。 */
    public ToDevice buildTo() {
        LabSessionHolder.Snapshot s = labSessionHolder.current();
        String  serverId  = pick(s != null ? s.getServerId()     : null, serverProps.getServerId());
        String  ip        = pick(s != null ? s.getServerIp()     : null, serverProps.getIp());
        int     port      = (s != null && s.getServerPort() != null) ? s.getServerPort() : serverProps.getPort();
        String  domain    = pick(s != null ? s.getServerDomain() : null, serverProps.getDomain());
        String  transport = pick(s != null ? s.getTransport()    : null, clientProps.getTransport());

        ToDevice to = new ToDevice();
        to.setUserId(serverId);
        to.setIp(ip);
        to.setPort(port);
        to.setHostAddress(ip + ":" + port);
        to.setRealm(extractRealm(domain));
        to.setTransport("TCP".equalsIgnoreCase(transport) ? "TCP" : "UDP");
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
     * 主动上报目录。channelCount/catalogName 传入值优先，否则取 {@link LabChannelHolder} 当前配置。
     * 通道名格式 {@code prefix + i}，与 {@code LabQueryListener.onCatalogQuery} 被动回应完全一致。
     */
    public String pushCatalog(int channelCount, String catalogName) {
        LabChannelHolder.Config cfg = labChannelHolder.current();
        int count = channelCount > 0 ? channelCount : cfg.getCount();
        String prefix = StringUtils.isNotBlank(catalogName) ? catalogName : cfg.getNamePrefix();
        List<DeviceItem> items = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            DeviceItem item = new DeviceItem();
            item.setDeviceId(clientProps.getClientId() + String.format("%02d", i));
            item.setName(prefix + i);   // 与 onCatalogQuery 同格式（去掉 "-ch"）
            item.setStatus("ON");
            item.setParental(0);
            item.setRegisterWay(1);
            item.setSafetyWay(0);
            items.add(item);
        }
        DeviceResponse resp = new DeviceResponse(CmdTypeEnum.CATALOG.getType(), "0", clientProps.getClientId());
        resp.setSumNum(count);
        resp.setDeviceItemList(items);
        log.info("Lab CATALOG → server, channelCount={}", count);
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
        alarm.setAlarmTime(new Date());

        DeviceAlarmNotify notify = new DeviceAlarmNotify(
            CmdTypeEnum.ALARM.getType(), RandomStrUtil.getValidationCode(), clientProps.getClientId());
        notify.setAlarm(alarm);
        log.info("Lab ALARM → server, alarmType={}", alarmType);
        return ClientCommandSender.sendAlarmCommand(buildFrom(), buildTo(), notify);
    }

    private String extractRealm(String id) {
        return (id != null && id.length() >= 8) ? id.substring(0, 8) : "34020000";
    }
}
