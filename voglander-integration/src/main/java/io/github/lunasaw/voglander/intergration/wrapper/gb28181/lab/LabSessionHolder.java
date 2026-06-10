package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * 协议验证台「当前注册会话」参数持有者（Lab 模式专用）。
 * <p>
 * 由 register 请求写入，供后续 keepalive/catalog/deviceInfo/alarm 复用同一目标与身份。
 * 快照为 null 表示未自定义 → 全部回退 sip.server.* / sip.client.*（本进程自环，行为同现状）。
 * keepalive 调度线程与 REST 注册线程并发读写，{@link AtomicReference} 整体替换保证一致快照。
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabSessionHolder {

    /** 不可变快照；字段 null/空 = 该项不覆盖，回退 props。 */
    @Getter
    @ToString
    @AllArgsConstructor
    public static class Snapshot {
        // 目标平台（To）
        private final String  serverId;
        private final String  serverIp;
        private final Integer serverPort;
        private final String  serverDomain;   // realm 初始构造来源
        private final String  transport;       // UDP / TCP
        // 设备身份（From）覆盖，可选
        private final String  clientId;
        private final String  clientPassword;
    }

    private final AtomicReference<Snapshot> ref = new AtomicReference<>();

    /** 是否带了任何目标或身份覆盖（controller 用以决定 apply / reset）。 */
    public static boolean hasOverride(String serverId, String serverIp, Integer serverPort,
        String serverDomain, String transport, String clientId, String clientPassword) {
        return StringUtils.isNotBlank(serverId) || StringUtils.isNotBlank(serverIp) || serverPort != null
            || StringUtils.isNotBlank(serverDomain) || StringUtils.isNotBlank(transport)
            || StringUtils.isNotBlank(clientId) || StringUtils.isNotBlank(clientPassword);
    }

    public void apply(Snapshot s) {
        ref.set(s);
        log.info("Lab 注册会话已更新: serverId={}, target={}:{}, transport={}, clientId={}",
            s.getServerId(), s.getServerIp(), s.getServerPort(), s.getTransport(), s.getClientId());
    }

    public void reset() {
        ref.set(null);
        log.info("Lab 注册会话已重置为自环");
    }

    public Snapshot current() {
        return ref.get();
    }
}
