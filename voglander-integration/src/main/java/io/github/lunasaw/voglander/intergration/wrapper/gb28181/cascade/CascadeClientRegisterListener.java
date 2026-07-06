package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterSuccessEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterFailureEvent;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
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
        String eventUserId = event.getUserId();
        CascadePlatformDTO platform = resolvePlatform(eventUserId);
        if (platform != null) {
            cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.ONLINE);
            publishRegisterStatus(platform.getPlatformId(), CascadeConstant.RegisterStatus.ONLINE);
            log.info("级联注册成功: eventUserId={}, platformId={}, localClientId={}",
                eventUserId, platform.getPlatformId(), platform.getLocalClientId());
        } else {
            log.warn("收到级联注册成功事件但未找到平台配置: eventUserId={}", eventUserId);
        }
    }

    @EventListener
    public void onRegisterFailure(ClientRegisterFailureEvent event) {
        String eventUserId = event.getUserId();
        CascadePlatformDTO platform = resolvePlatform(eventUserId);
        if (platform != null) {
            cascadePlatformManager.updateRegisterStatus(platform.getId(), CascadeConstant.RegisterStatus.FAILED);
            publishRegisterStatus(platform.getPlatformId(), CascadeConstant.RegisterStatus.FAILED);
            log.warn("级联注册失败: eventUserId={}, platformId={}, localClientId={}, statusCode={}",
                eventUserId, platform.getPlatformId(), platform.getLocalClientId(), event.getStatusCode());
        } else {
            log.warn("收到级联注册失败事件但未找到平台配置: eventUserId={}, statusCode={}",
                eventUserId, event.getStatusCode());
        }
    }

    /**
     * 框架注册响应事件的 userId 来自 REGISTER 响应 To 头，通常是上级平台 platformId；
     * 兼容旧测试/旧事件里传 localClientId 的情况，避免状态卡在 REGISTERING。
     */
    private CascadePlatformDTO resolvePlatform(String eventUserId) {
        if (eventUserId == null) {
            return null;
        }
        CascadePlatformDTO platform = cascadePlatformManager.getByPlatformId(eventUserId);
        return platform != null ? platform : cascadePlatformManager.getByLocalClientId(eventUserId);
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
