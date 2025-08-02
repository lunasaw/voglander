package io.github.lunasaw.voglander.intergration.wrapper.gb28181.start;

import javax.sip.SipListener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.luna.common.os.SystemInfoUtil;

import io.github.lunasaw.gbproxy.client.config.SipClientProperties;
import io.github.lunasaw.gbproxy.server.config.SipServerProperties;
import io.github.lunasaw.sip.common.layer.SipLayer;

/**
 * @author luna
 * @date 2023/12/29
 */

@Component
@ConditionalOnProperty(value = "sip.enable", havingValue = "true")
public class ServerStart implements CommandLineRunner {
    @Autowired
    private SipServerProperties sipServerProperties;

    @Autowired
    private SipClientProperties sipClientProperties;

    @Autowired
    private SipServerConfig     sipServerConfig;

    @Autowired
    private SipLayer            sipLayer;

    @Autowired
    private SipListener         sipListener;

    @Override
    public void run(String... args) {
        String ip = sipServerProperties.getIp();
        if (StringUtils.isBlank(ip)) {
            ip = SystemInfoUtil.getIpv4();
        }
        sipLayer.setSipListener(sipListener);
        sipLayer.addListeningPoint(ip, sipServerProperties.getPort(), sipServerConfig.getEnableLog());
        String domain = sipClientProperties.getDomain();
        if (StringUtils.isBlank(domain)) {
            domain = SystemInfoUtil.getIpv4();
        }
        sipLayer.addListeningPoint(domain, sipClientProperties.getPort(), sipServerConfig.getEnableLog());
    }
}
