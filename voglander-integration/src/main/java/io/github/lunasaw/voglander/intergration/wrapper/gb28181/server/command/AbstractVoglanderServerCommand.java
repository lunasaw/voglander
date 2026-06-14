package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import com.luna.common.dto.constant.ResultCode;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommand;
import io.github.lunasaw.sipgateway.core.api.envelope.GatewayCommandResult;
import io.github.lunasaw.sipgateway.core.core.CommandHandlerRegistry;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.trace.SipMessageTracer;
import io.github.lunasaw.voglander.manager.routing.DeviceNodeRouteService;
import io.github.lunasaw.voglander.manager.routing.InternalCommandForwardService;
import io.github.lunasaw.voglander.manager.routing.NodeAliveService;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181服务端指令抽象基类
 * <p>
 * 提供统一的异常处理和日志记录功能，所有具体的服务端指令实现类都应该继承此抽象类。
 * </p>
 *
 * <h3>sip-gateway 1.8.0 envelope 接入（2026-06-01）</h3>
 * <p>
 * 出站命令统一经 {@link CommandHandlerRegistry#require(String)} 取得 {@code CommandHandler}，
 * 再调用 {@code handle(GatewayCommand)} 由协议适配层（{@code Gb28181WhitelistHandlers} / {@code Gb28181CommandSpecs}）
 * 反查 payload schema、转 SIP 消息下发。子类只经 {@link #dispatchEnvelope} 出站。
 * </p>
 *
 * <h3>子类典型用法</h3>
 *
 * <pre>{@code
 * public ResultDTO<Void> queryDeviceInfo(String deviceId) {
 *     validateDeviceId(deviceId, "设备ID不能为空");
 *     return dispatchEnvelope(Gb28181CommandType.QUERY_DEVICE_INFO.type(), deviceId, Map.of());
 * }
 * }</pre>
 *
 * @author luna
 * @since 2025/8/2
 * @version 2.0
 */
@Slf4j
public abstract class AbstractVoglanderServerCommand {

    /**
     * 1.8.0 envelope 命令调度入口。子类通过此 Registry 走统一通道。
     */
    @Autowired
    public CommandHandlerRegistry  commandHandlerRegistry;

    /** 命令亲和路由（开关关闭时 bean 不存在，required=false） */
    @Autowired(required = false)
    private DeviceNodeRouteService      deviceNodeRouteService;

    @Autowired(required = false)
    private NodeAliveService            nodeAliveService;

    @Autowired(required = false)
    private InternalCommandForwardService internalCommandForwardService;

    /**
     * 经 envelope 通道下发命令的模板方法。
     * <p>
     * 步骤：
     * <ol>
     * <li>校验 type/payload 合法</li>
     * <li>构造 {@link GatewayCommand}（{@code requestId} 留空，由 gateway 内部生成）</li>
     * <li>{@link CommandHandlerRegistry#require(String)} 解析 type 对应 handler；type 不存在抛 {@code ResponseStatusException(404)}</li>
     * <li>调用 {@code handler.handle(cmd)} 拿到 {@link GatewayCommandResult#correlationId()}（sn/callId）</li>
     * <li>异常统一捕获并转为 {@link ResultDTOUtils#failure} 返回</li>
     * </ol>
     * </p>
     *
     * @param type     三段式命令类型，如 {@code gb28181.Control.Ptz}
     * @param deviceId 设备ID，对 INVITE/Bye 等 callId 类命令可为 null
     * @param payload  命令负载，schema 严格按 spec 字段约束
     * @return ResultDTO&lt;Void&gt; 调用成功（带 callId 日志）或异常摘要
     */
    protected ResultDTO<Void> dispatchEnvelope(String type, String deviceId, Map<String, Object> payload) {
        try {
            log.debug("envelope::开始下发命令, type={}, deviceId={}, payload={}", type, deviceId, payload);
            GatewayCommand cmd = new GatewayCommand(type, deviceId, payload, null);

            // 路由判断（开关关闭时跳过）
            if (deviceNodeRouteService != null && nodeAliveService != null && deviceId != null) {
                String target = deviceNodeRouteService.lookupNode(deviceId);
                if (target != null && !target.equals(nodeAliveService.getLocalNodeId())
                        && nodeAliveService.isAlive(target)) {
                    return internalCommandForwardService.forward(target, cmd);
                }
            }

            GatewayCommandResult result = commandHandlerRegistry.require(type).handle(cmd);
            String callId = result == null ? null : result.correlationId();
            // 协议层出站消息链路追踪：完整 payload + callId/deviceId
            SipMessageTracer.send(type, deviceId, callId, payload);
            log.info("envelope::命令下发成功, type={}, deviceId={}, callId={}", type, deviceId, callId);
            return ResultDTOUtils.success();
        } catch (Exception e) {
            log.error("envelope::命令下发失败, type={}, deviceId={}, payload={}", type, deviceId, payload, e);
            return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 经 envelope 通道下发命令并返回 callId 类型结果。
     *
     * @param type     三段式命令类型
     * @param deviceId 设备ID
     * @param payload  命令负载
     * @return ResultDTO&lt;String&gt; 成功时 data 为 callId
     */
    protected ResultDTO<String> dispatchEnvelopeWithCallId(String type, String deviceId, Map<String, Object> payload) {
        try {
            log.debug("envelope::开始下发命令(返回 callId), type={}, deviceId={}, payload={}", type, deviceId, payload);
            GatewayCommand cmd = new GatewayCommand(type, deviceId, payload, null);
            GatewayCommandResult result = commandHandlerRegistry.require(type).handle(cmd);
            String callId = result == null ? null : result.correlationId();
            // 协议层出站消息链路追踪：完整 payload + callId/deviceId
            SipMessageTracer.send(type, deviceId, callId, payload);
            log.info("envelope::命令下发成功, type={}, deviceId={}, callId={}", type, deviceId, callId);
            return ResultDTOUtils.success(callId);
        } catch (Exception e) {
            log.error("envelope::命令下发失败, type={}, deviceId={}, payload={}", type, deviceId, payload, e);
            return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, e.getMessage(), null);
        }
    }

    /**
     * 参数校验工具方法
     */
    protected void validateDeviceId(String deviceId, String message) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException(message != null ? message : "设备ID不能为空");
        }
    }

    /**
     * 参数校验工具方法
     */
    protected void validateNotNull(Object param, String message) {
        if (param == null) {
            throw new IllegalArgumentException(message != null ? message : "参数不能为空");
        }
    }
}