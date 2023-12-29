package io.github.lunasaw.voglander.intergration.wrapper.gb28181.start;

import com.luna.common.os.SystemInfoUtil;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author luna
 * @date 2023/12/29
 */

@Component
public class ServerStart implements CommandLineRunner {

    public static Map<String, Device> DEVICE_MAP = new ConcurrentHashMap<>();
    public static Map<String, Device> DEVICE_SERVER_VIEW_MAP = new ConcurrentHashMap<>();
    @Autowired
    private SipServerConfig sipServerConfig;

    @Override
    public void run(String... args) throws Exception {
        String ip = sipServerConfig.getIp();
        if (StringUtils.isBlank(ip)) {
            ip = SystemInfoUtil.getIpv4();
        }
        SipLayer.addListeningPoint(ip, sipServerConfig.getPort(), sipServerConfig.getEnableLog());

        FromDevice serverFrom = FromDevice.getInstance("41010500002000000001", ip, sipServerConfig.getPort());
        serverFrom.setPassword("bajiuwulian1006");
        serverFrom.setRealm("4101050000");

        DEVICE_MAP.put("server_from", serverFrom);
    }
}
