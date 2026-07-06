package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import org.springframework.stereotype.Component;

/**
 * 为级联上级平台构造 SIP FromDevice / ToDevice。
 * 与现有 {@code VoglanderClientDeviceSupplier} 并列，专门服务级联场景。
 */
@Component
public class CascadeDeviceSupplier {

    public FromDevice buildFromDevice(CascadePlatformDTO platform) {
        FromDevice from = new FromDevice();
        from.setUserId(platform.getLocalClientId());
        // localIp/localPort 由 Controller 在 add/enable 时回填保证不为空
        from.setIp(platform.getLocalIp());
        from.setPort(platform.getLocalPort() != null ? platform.getLocalPort() : 5061);
        from.setRealm(extractRealm(platform.getLocalClientId()));
        from.setPassword(platform.getPassword());
        return from;
    }

    public ToDevice buildToDevice(CascadePlatformDTO platform) {
        int port = platform.getPlatformPort() != null ? platform.getPlatformPort() : 5060;
        ToDevice to = new ToDevice();
        to.setUserId(platform.getPlatformId());
        to.setIp(platform.getPlatformIp());
        to.setPort(port);
        to.setHostAddress(platform.getPlatformIp() + ":" + port);
        to.setRealm(platform.getPlatformDomain());
        to.setTransport(platform.getTransport() != null ? platform.getTransport() : "UDP");
        to.setCharset(platform.getCharset() != null ? platform.getCharset() : "GB2312");
        to.setPassword(platform.getPassword());
        return to;
    }

    private String extractRealm(String deviceId) {
        return (deviceId != null && deviceId.length() >= 8) ? deviceId.substring(0, 8) : "34020000";
    }
}
