package io.github.lunasaw.voglander.manager.event;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 入站事件分发器（Phase 3 / PROTOCOL S1）。
 * <p>
 * 注入所有 {@link ProtocolEventHandler} bean，建立 {@code protocol → handler} 映射；
 * 按 {@link DeviceEvent#protocol()} 路由。新增协议只需新增一个 {@link ProtocolEventHandler} 实现，
 * 本类<strong>零改动</strong>。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
public class InboundEventDispatcher {

    private final Map<String, ProtocolEventHandler> handlers;

    public InboundEventDispatcher(List<ProtocolEventHandler> handlerList) {
        this.handlers = handlerList.stream()
            .collect(Collectors.toMap(ProtocolEventHandler::protocol, h -> h));
        log.info("InboundEventDispatcher 就绪，已注册协议: {}", handlers.keySet());
    }

    /**
     * 按 protocol 段路由到对应 handler。空事件、缺协议标识、无匹配 handler 均安全丢弃。
     */
    public void dispatch(DeviceEvent event) {
        if (event == null || event.protocol() == null) {
            log.warn("收到空事件或缺协议标识，忽略");
            return;
        }
        ProtocolEventHandler handler = handlers.get(event.protocol());
        if (handler == null) {
            log.warn("无 {} 协议的事件处理器，丢弃事件 type={}", event.protocol(), event.type());
            return;
        }
        handler.handle(event);
    }
}
