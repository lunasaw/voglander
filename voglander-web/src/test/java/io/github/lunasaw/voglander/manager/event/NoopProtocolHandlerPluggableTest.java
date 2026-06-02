package io.github.lunasaw.voglander.manager.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 8：NoopProtocolHandler 可插拔验证（PROTOCOL 扩展性关键验证点）。
 * <p>
 * 校验新增协议 handler 零改动注入到 {@link InboundEventDispatcher}，路由生效。
 * 本测试用 noop handler 模拟新增协议（如 onvif），验证插拔性。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class NoopProtocolHandlerPluggableTest {

    @Test
    public void testNoopHandlerPlugsIntoDispatcher() {
        ProtocolEventHandler gb = Mockito.mock(ProtocolEventHandler.class);
        Mockito.when(gb.protocol()).thenReturn("gb28181");

        // 模拟新增 onvif handler（noop 实现）
        ProtocolEventHandler noop = Mockito.mock(ProtocolEventHandler.class);
        Mockito.when(noop.protocol()).thenReturn("onvif");

        // dispatcher 自动注册两个 handler（通过 List<ProtocolEventHandler> 构造注入）
        InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of(gb, noop));

        DeviceEvent onvifEvent = new DeviceEvent("onvif", "Device", "Register", "dev-1", null, 1000L, null, "node-1");
        dispatcher.dispatch(onvifEvent);

        // 新增协议事件被路由到 noop handler，gb handler 不触碰
        verify(noop, times(1)).handle(onvifEvent);
        verify(gb, times(0)).handle(Mockito.any());

        log.info("NoopProtocolHandler 可插拔验证通过：新增协议零改动注入 dispatcher");
    }

    @Test
    public void testNoopHandlerSafelyIgnoresEvents() {
        // 实际的 noop handler 实现（Phase 8 将创建真实类）应安全吞掉所有事件
        NoopProtocolHandler noop = new NoopProtocolHandler("test-protocol");
        DeviceEvent event = new DeviceEvent("test-protocol", "Any", "Event", "dev-1", null, 1000L, null, "node-1");

        assertDoesNotThrow(() -> noop.handle(event), "NoopProtocolHandler 应安全处理任意事件");
        log.info("NoopProtocolHandler 安全吞事件验证通过");
    }
}
