package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import java.util.HashMap;
import java.util.Map;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientInviteEvent;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.config.properties.VoglanderSipClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lab INVITE 监听器：平台向设备 UA 发送点播请求时
 * <ol>
 *   <li>解析收流目标（ip/port/ssrc/transport）；</li>
 *   <li>同步回 200 OK 应答（必须及时，平台 8s 超时 + 事务 32s 失效）；</li>
 *   <li>推 SSE {@code clientcmd.invite}（含媒体目标，供前端展示/手动推流）；</li>
 *   <li>{@code push.auto=true} 时立即起 ffmpeg 推流，完成媒体闭环。</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voglander.protocol-lab.enabled", havingValue = "true")
public class LabInviteListener {

    private final ApplicationEventPublisher eventPublisher;
    private final LabMediaPushService       pushService;
    private final VoglanderSipClientProperties clientProps;
    private final LabChannelHolder          labChannelHolder;

    @EventListener
    public void onInvite(ClientInviteEvent e) {
        // 0. 判断：是否是Lab设备的INVITE（设备ID或通道ID）
        String userId = e.getUserId();
        if (!isLabDevice(userId)) {
            log.debug("Lab INVITE过滤: userId={} 不是Lab设备或通道, clientId={}", userId, clientProps.getClientId());
            return; // 不是Lab设备，不处理
        }

        log.info("Lab 收到 INVITE: userId={}, callId={}", userId, e.getCallId());

        // 1. 解析收流目标（SDP 可能为 null，service 内部兜底）
        LabInviteTarget target = pushService.parseTarget(e);

        // 2. 同步回 200 OK + 缓存目标（无论自动/手动都要回，否则平台拿不到应答）
        pushService.acceptInvite(target);

        // 3. 推 SSE（保留现状字段 + 增加收流目标）
        Map<String, Object> d = new HashMap<>();
        d.put("callId", target.getCallId() != null ? target.getCallId() : "");
        d.put("clientId", target.getUserId() != null ? target.getUserId() : "");
        d.put("mediaIp", target.getMediaIp());
        d.put("mediaPort", target.getMediaPort());
        d.put("ssrc", target.getSsrc());
        d.put("ts", System.currentTimeMillis());
        log.debug("Lab 收到 INVITE, callId={}, 推流目标={}:{}", target.getCallId(), target.getMediaIp(), target.getMediaPort());
        eventPublisher.publishEvent(new SseRelayEvent("clientcmd.invite", d));

        // 4. 自动模式：立即起 ffmpeg（用配置默认 ffmpeg/file）
        if (pushService.isAutoPush()) {
            try {
                pushService.startPush(target, null, null, null);
            } catch (Exception ex) {
                log.warn("Lab 自动推流启动失败, callId={}: {}", target.getCallId(), ex.getMessage());
            }
        }
    }

    /**
     * 判断userId是否是Lab设备（clientId 或其下属通道）
     */
    private boolean isLabDevice(String userId) {
        if (userId == null) {
            return false;
        }
        // 1. 精确匹配clientId
        if (userId.equals(clientProps.getClientId())) {
            return true;
        }
        // 2. 判断是否是Lab的通道ID
        return labChannelHolder.ownsChannel(clientProps.getClientId(), userId);
    }
}
