package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterSuccessEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterFailureEvent;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听 GB28181 客户端注册事件，同步更新 tb_cascade_platform.register_status。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CascadeClientRegisterListener {

    private final CascadePlatformManager cascadePlatformManager;

    @EventListener
    public void onRegisterSuccess(ClientRegisterSuccessEvent event) {
        String localClientId = event.getUserId();
        CascadePlatformDTO platform = cascadePlatformManager.getByLocalClientId(localClientId);
        if (platform != null) {
            cascadePlatformManager.updateRegisterStatus(platform.getId(), 1); // ONLINE
            log.info("级联注册成功: localClientId={}", localClientId);
        }
    }

    @EventListener
    public void onRegisterFailure(ClientRegisterFailureEvent event) {
        String localClientId = event.getUserId();
        CascadePlatformDTO platform = cascadePlatformManager.getByLocalClientId(localClientId);
        if (platform != null) {
            cascadePlatformManager.updateRegisterStatus(platform.getId(), 3); // FAILED
            log.warn("级联注册失败: localClientId={}, statusCode={}", localClientId, event.getStatusCode());
        }
    }
}
