package io.github.lunasaw.voglander.intergration.wrapper.gb28181.start;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.luna.common.os.SystemInfoUtil;

import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.layer.SipLayer;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.manager.DeviceConfigManager;

/**
 * @author luna
 * @date 2023/12/29
 */

@Component
@ConditionalOnProperty(value = "sip.enable", havingValue = "true")
public class ServerStart implements CommandLineRunner {

    // TODO 优化 server_from 应该是发送配置落表
    public static Map<String, Device> DEVICE_MAP = new ConcurrentHashMap<>();
    @Autowired
    private SipServerConfig           sipServerConfig;

    @Autowired
    private DeviceConfigManager       deviceConfigManager;

    @Autowired
    private SipLayer                  sipLayer;

    @Override
    public void run(String... args) {
        String ip = sipServerConfig.getIp();
        if (StringUtils.isBlank(ip)) {
            ip = SystemInfoUtil.getIpv4();
        }
        sipLayer.addListeningPoint(ip, sipServerConfig.getPort(), sipServerConfig.getEnableLog());

        String sip =
            deviceConfigManager.getSystemValueWithDefault(DeviceConstant.LocalConfig.DEVICE_GB_SIP, DeviceConstant.LocalConfig.DEVICE_GB_SIP_DEFAULT);
        String password = deviceConfigManager.getSystemValueWithDefault(DeviceConstant.LocalConfig.DEVICE_GB_PASSWORD,
            DeviceConstant.LocalConfig.DEVICE_GB_PASSWORD_DEFAULT);
        FromDevice serverFrom = FromDevice.getInstance(sip, ip, sipServerConfig.getPort());
        serverFrom.setPassword(password);
        serverFrom.setRealm(sip.substring(0, 9));

        DEVICE_MAP.put("server_from", serverFrom);
    }
}
