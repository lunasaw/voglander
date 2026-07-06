package io.github.lunasaw.voglander.intergration.wrapper.gb28181.start;

import javax.sip.SipListener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.luna.common.os.SystemInfoUtil;

import io.github.lunasaw.gbproxy.client.config.SipClientProperties;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipServerProperties;

/**
 * SIP 监听点启动器。
 *
 * <p><b>绑定地址 / 通告地址解耦</b>：监听点的 socket 绑定地址取自各自的 {@code listenIp}
 * （默认 {@code 0.0.0.0}，即本机所有网卡），与对外通告的 {@code ip}/{@code domain} 分离。
 * 这样：
 * <ul>
 *   <li>任意网段（127 / 10.x / 192.x …）的对端都能抵达本机监听点；</li>
 *   <li>切换网络后旧 IP 从网卡消失不再触发发包 {@code BindException}；</li>
 *   <li>Via/Contact/From 仍通告真实 {@code ip}/{@code domain}，对端可正常回包。</li>
 * </ul>
 * server(5060) 与 client(5061) 同绑 {@code 0.0.0.0} 时共用一个 SipStack，但 SipLayer 的
 * Provider 注册表按 {@code ip:port} 区分，出站请求按 Via 端口感知选取监听点，互不串扰。
 *
 * @author luna
 * @date 2023/12/29
 */
@Component
@ConditionalOnProperty(value = "sip.enable", havingValue = "true")
public class ServerStart implements CommandLineRunner {

    @Autowired
    private VoglanderSipServerProperties voglanderServerProperties;

    @Autowired
    private VoglanderSipClientProperties voglanderClientProperties;

    @Autowired
    private SipClientProperties          sipClientProperties;

    @Autowired
    private SipServerConfig              sipServerConfig;

    @Autowired
    private SipLayer                     sipLayer;

    @Autowired
    private SipListener                  sipListener;

    @Override
    public void run(String... args) {
        // 服务端监听点：绑定地址用 listenIp（默认 0.0.0.0=全网卡），与对外通告 ip 解耦
        String serverListenIp = resolveListenIp(voglanderServerProperties.getListenIp());
        sipLayer.addListeningPoint(serverListenIp, voglanderServerProperties.getPort(), sipListener,
            sipServerConfig.getEnableLog());

        // 客户端监听点：同理绑 listenIp（默认 0.0.0.0）
        String clientListenIp = resolveListenIp(voglanderClientProperties.getListenIp());
        sipLayer.addListeningPoint(clientListenIp, voglanderClientProperties.getPort(), sipListener,
            sipServerConfig.getEnableLog());

        // 兼容：框架 client 属性的 domain 用于其内部通告，留空时回填本机真实 IP（非绑定地址）
        if (StringUtils.isBlank(sipClientProperties.getDomain())) {
            sipClientProperties.setDomain(SystemInfoUtil.getNoLoopbackIP());
        }
    }

    /**
     * 解析绑定地址：留空回退 {@code 0.0.0.0}（全网卡），非空则按显式配置绑定。
     */
    private String resolveListenIp(String listenIp) {
        return StringUtils.isBlank(listenIp) ? "0.0.0.0" : listenIp;
    }
}
