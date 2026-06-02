package io.github.lunasaw.voglander.manager.event;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Noop 协议事件处理器（Phase 8 / PROTOCOL 扩展性验证）。
 * <p>
 * 安全吞掉所有事件，不做任何处理。用于：
 * </p>
 * <ul>
 * <li>新增协议的<strong>可插拔验证</strong>：通过 {@code @Component} 或测试构造注入到 dispatcher，
 * 验证新增 handler 零改动即可路由生效；</li>
 * <li>协议降级/灰度：临时禁用某协议的业务处理（注册 noop 替换实际 handler）。</li>
 * </ul>
 * <p>
 * 设计原则：<strong>安全第一</strong> —— 任何事件、任何负载均不抛异常，符合 "无副作用" 语义。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class NoopProtocolHandler implements ProtocolEventHandler {

    private final String protocol;

    public NoopProtocolHandler(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public void handle(DeviceEvent event) {
        // Noop：安全吞掉事件，不做任何处理（满足可插拔验证 + 协议降级场景）
        log.debug("NoopProtocolHandler 收到事件并安全忽略 - protocol={}, type={}, deviceId={}",
            protocol, event.type(), event.deviceId());
    }
}
