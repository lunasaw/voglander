package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.subscribe;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.common.constant.device.SubscriptionConstant;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.command.Gb28181CommandType;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181-2022 §9.11 平台端发起的设备订阅出站命令封装。
 * <p>
 * 全部经 {@link #dispatchEnvelopeWithCallId} 同步取得 SUBSCRIBE dialog 的 callId（供 refresh/unsubscribe 复用 dialog），
 * 命令 type 逐字对齐框架注册键（{@code gb28181.Subscribe.*}，已字节码核验），payload 键与框架 handler 读取键一致。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class VoglanderServerSubscribeCommand extends AbstractVoglanderServerCommand {

    /**
     * 目录订阅。
     *
     * @param deviceId 设备国标编码
     * @param expires  订阅有效期(秒)
     * @return data 为 SUBSCRIBE dialog callId
     */
    public ResultDTO<String> subscribeCatalog(String deviceId, int expires) {
        validateDeviceId(deviceId, "目录订阅设备ID不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("expires", expires);
        payload.put("eventType", SubscriptionConstant.EVENT_CATALOG);
        return dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_CATALOG.type(), deviceId, payload);
    }

    /**
     * 移动位置订阅。
     *
     * @param deviceId 设备国标编码
     * @param interval 位置上报间隔(秒)
     * @param expires  订阅有效期(秒)
     * @return data 为 SUBSCRIBE dialog callId
     */
    public ResultDTO<String> subscribeMobilePosition(String deviceId, int interval, int expires) {
        validateDeviceId(deviceId, "位置订阅设备ID不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("interval", String.valueOf(interval));
        payload.put("expires", expires);
        payload.put("eventType", SubscriptionConstant.EVENT_MOBILE_POSITION);
        return dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_MOBILE_POSITION.type(), deviceId, payload);
    }

    /**
     * 告警订阅（本期不带过滤，全量订阅）。
     *
     * @param deviceId 设备国标编码
     * @param expires  订阅有效期(秒)
     * @return data 为 SUBSCRIBE dialog callId
     */
    public ResultDTO<String> subscribeAlarm(String deviceId, int expires) {
        validateDeviceId(deviceId, "告警订阅设备ID不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("expires", expires);
        payload.put("eventType", SubscriptionConstant.EVENT_ALARM);
        return dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_ALARM.type(), deviceId, payload);
    }

    /**
     * 续订（复用 dialog callId，过期前刷新）。
     * <p>
     * 该命令按 dialog 寻址，框架内部以 callId 找 dialog；deviceId 实参传 callId 仅用于亲和路由 key 与 trace。
     * </p>
     *
     * @param callId  SUBSCRIBE dialog callId
     * @param expires 续订有效期(秒)
     * @return data 为 callId
     */
    public ResultDTO<String> refresh(String callId, int expires) {
        validateNotNull(callId, "续订callId不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", callId);
        payload.put("expires", expires);
        return dispatchEnvelopeWithCallId(Gb28181CommandType.SUBSCRIBE_REFRESH.type(), callId, payload);
    }

    /**
     * 撤销订阅（expires=0，按 dialog 寻址）。
     *
     * @param callId SUBSCRIBE dialog callId
     */
    public ResultDTO<Void> unsubscribe(String callId) {
        validateNotNull(callId, "撤销订阅callId不能为空");
        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", callId);
        return dispatchEnvelope(Gb28181CommandType.SUBSCRIBE_UNSUBSCRIBE.type(), callId, payload);
    }
}
