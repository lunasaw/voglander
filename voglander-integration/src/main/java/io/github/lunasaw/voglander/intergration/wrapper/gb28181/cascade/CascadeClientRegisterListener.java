package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterSuccessEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterFailureEvent;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听 GB28181 客户端注册事件，同步更新 tb_cascade_platform.register_status，
 * 并通过 SSE topic {@code cascade.register} 推送状态变更给前端（前端列表实时刷新）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CascadeClientRegisterListener {

    private final CascadePlatformManager    cascadePlatformManager;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onRegisterSuccess(ClientRegisterSuccessEvent event) {
        String localClientId = event.getUserId();
        CascadePlatformDTO platform = cascadePlatformManager.getByLocalClientId(localClientId);
        if (platform != null) {
            cascadePlatformManager.updateRegisterStatus(platform.getId(), 1); // ONLINE
            publishRegisterStatus(platform.getPlatformId(), 1);
            log.info("级联注册成功: localClientId={}", localClientId);
        }
    }

    @EventListener
    public void onRegisterFailure(ClientRegisterFailureEvent event) {
        String localClientId = event.getUserId();
        CascadePlatformDTO platform = cascadePlatformManager.getByLocalClientId(localClientId);
        if (platform != null) {
            cascadePlatformManager.updateRegisterStatus(platform.getId(), 3); // FAILED
            publishRegisterStatus(platform.getPlatformId(), 3);
            log.warn("级联注册失败: localClientId={}, statusCode={}", localClientId, event.getStatusCode());
        }
    }

    /**
     * 推送注册状态变更到 SSE topic {@code cascade.register}。
     *
     * @param platformId     上级平台国标 ID
     * @param registerStatus 注册状态（0离线 1在线 2注册中 3失败）
     */
    private void publishRegisterStatus(String platformId, int registerStatus) {
        Map<String, Object> data = new HashMap<>();
        data.put("platformId", platformId);
        data.put("registerStatus", registerStatus);
        data.put("ts", System.currentTimeMillis());
        eventPublisher.publishEvent(new SseRelayEvent("cascade.register", data));
    }
}
