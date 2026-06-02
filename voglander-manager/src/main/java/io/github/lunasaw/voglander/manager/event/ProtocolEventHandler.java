package io.github.lunasaw.voglander.manager.event;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;

/**
 * 协议事件处理器（Phase 3 / PROTOCOL S1）。
 * <p>
 * 每种协议一个实现，按 {@code protocol} 注册到 {@link InboundEventDispatcher}。
 * 实现内部按 {@code group/name} 二次路由到协议无关的业务服务
 * （{@code DeviceRegisterService}/{@code DeviceManager}/{@code MediaSessionManager}）。
 * </p>
 *
 * @author luna
 */
public interface ProtocolEventHandler {

    /**
     * 本 handler 支持的协议标识，对应 {@link DeviceEvent#protocol()}，如 {@code "gb28181"}。
     */
    String protocol();

    /**
     * 处理本协议下的一条事件。
     */
    void handle(DeviceEvent event);
}
