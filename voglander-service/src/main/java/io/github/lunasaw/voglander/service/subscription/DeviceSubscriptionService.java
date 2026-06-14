package io.github.lunasaw.voglander.service.subscription;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.subscribe.VoglanderServerSubscribeCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceSubscriptionDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.DeviceSubscriptionManager;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181-2022 设备订阅编排服务（位置 / 目录 / 告警）。
 * <p>
 * 职责：开/关订阅（意图持久化 + 在线即刻下发 SUBSCRIBE / 离线 PENDING）、注册即重订阅（需求 4）、续订。
 * 意图与运行态分离：开关改意图，意图驱动 SUBSCRIBE 下发，callId 同步回填供 refresh/unsubscribe 复用 dialog。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Service
public class DeviceSubscriptionService {

    @Autowired
    private DeviceSubscriptionManager       subscriptionManager;

    @Autowired
    private DeviceManager                   deviceManager;

    @Autowired
    private VoglanderServerSubscribeCommand subscribeCommand;

    /**
     * 订阅总开关：关闭则 toggle/重订阅/续订均仅持久化意图、不向设备下发 SUBSCRIBE（方案 §8）。
     */
    @Value("${gateway.gb28181.subscription.enabled:true}")
    private boolean                         subscriptionEnabled;

    @Value("${gateway.gb28181.subscription.default-expires:" + SubscriptionConstant.DEFAULT_EXPIRES + "}")
    private int                             defaultExpires;

    @Value("${gateway.gb28181.subscription.position-interval:" + SubscriptionConstant.DEFAULT_POSITION_INTERVAL + "}")
    private int                             positionInterval;

    /**
     * 开启订阅：持久化意图；总开关开启 + 设备在线则即刻下发 SUBSCRIBE，否则标 PENDING 等注册钩子补发。
     */
    public boolean enable(String deviceId, SubscriptionConstant.Type type) {
        subscriptionManager.upsertIntent(deviceId, type, true);
        if (!subscriptionEnabled) {
            subscriptionManager.markPending(deviceId, type);
            log.info("订阅总开关关闭，仅持久化意图不下发, deviceId={}, type={}", deviceId, type);
            return true;
        }
        DeviceDTO device = deviceManager.getDtoByDeviceId(deviceId);
        if (device == null || !Objects.equals(device.getStatus(), DeviceConstant.Status.ONLINE)) {
            subscriptionManager.markPending(deviceId, type);
            log.info("设备离线，订阅意图已持久化待注册补发, deviceId={}, type={}", deviceId, type);
            return true;
        }
        return doSubscribe(deviceId, type);
    }

    /**
     * 关闭订阅：尽力撤销 dialog（失败不阻断）+ 关闭意图 + 标 INACTIVE。
     */
    public boolean disable(String deviceId, SubscriptionConstant.Type type) {
        DeviceSubscriptionDTO sub = subscriptionManager.getByDeviceAndType(deviceId, type);
        if (sub != null && sub.getCallId() != null
            && Objects.equals(sub.getStatus(), SubscriptionConstant.Status.ACTIVE)) {
            try {
                subscribeCommand.unsubscribe(sub.getCallId());
            } catch (Exception e) {
                log.warn("撤销订阅下发失败（忽略，继续关闭意图）, deviceId={}, type={}, 错误={}", deviceId, type, e.getMessage());
            }
        }
        subscriptionManager.upsertIntent(deviceId, type, false);
        subscriptionManager.markInactive(deviceId, type);
        return true;
    }

    /**
     * 注册即重订阅（需求 4）：遍历意图开启的订阅，重新 SUBSCRIBE 刷新 callId（旧 dialog 已随设备重启失效）。
     */
    public void resubscribeOnRegister(String deviceId) {
        if (!subscriptionEnabled) {
            return;
        }
        for (DeviceSubscriptionDTO sub : subscriptionManager.listEnabledByDevice(deviceId)) {
            try {
                doSubscribe(deviceId, SubscriptionConstant.Type.valueOf(sub.getSubType()));
            } catch (Exception e) {
                log.warn("注册重订阅单类失败, deviceId={}, type={}, 错误={}", deviceId, sub.getSubType(), e.getMessage());
            }
        }
    }

    /**
     * 续订：过期前 refresh，失败回退为完整 SUBSCRIBE。
     */
    public void refreshExpiring() {
        if (!subscriptionEnabled) {
            return;
        }
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().plusSeconds(SubscriptionConstant.REFRESH_AHEAD_SECONDS);
        for (DeviceSubscriptionDTO sub : subscriptionManager.listExpiring(threshold)) {
            SubscriptionConstant.Type type = SubscriptionConstant.Type.valueOf(sub.getSubType());
            ResultDTO<String> r = subscribeCommand.refresh(sub.getCallId(), defaultExpires);
            if (r != null && r.isSuccess()) {
                subscriptionManager.markActive(sub.getDeviceId(), type, sub.getCallId(), defaultExpires);
            } else {
                log.info("续订失败，回退重发完整 SUBSCRIBE, deviceId={}, type={}", sub.getDeviceId(), type);
                doSubscribe(sub.getDeviceId(), type);
            }
        }
    }

    /**
     * 下发一类订阅 SUBSCRIBE，成功回填 callId/expireTime（markActive），失败 markFailed。
     */
    private boolean doSubscribe(String deviceId, SubscriptionConstant.Type type) {
        ResultDTO<String> r;
        switch (type) {
            case CATALOG:
                r = subscribeCommand.subscribeCatalog(deviceId, defaultExpires);
                break;
            case MOBILE_POSITION:
                r = subscribeCommand.subscribeMobilePosition(deviceId, positionInterval, defaultExpires);
                break;
            case ALARM:
                r = subscribeCommand.subscribeAlarm(deviceId, defaultExpires);
                break;
            default:
                return false;
        }
        if (r != null && r.isSuccess() && r.getData() != null) {
            subscriptionManager.markActive(deviceId, type, r.getData(), defaultExpires);
            return true;
        }
        subscriptionManager.markFailed(deviceId, type);
        return false;
    }
}
