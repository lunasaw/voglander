package io.github.lunasaw.voglander.manager.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 3：入站事件分发器单元测试（PROTOCOL S1）。
 * <p>
 * 校验 {@link InboundEventDispatcher} 按 {@code protocol} 段路由到对应 {@link ProtocolEventHandler}，
 * 未知协议安全丢弃、空事件不抛异常。新增协议只需新增 handler，本类零改动。
 * </p>
 *
 * @author luna
 */
@Slf4j
public class InboundEventDispatcherTest {

    private DeviceEvent event(String protocol) {
        return new DeviceEvent(protocol, "Lifecycle", "Register", "dev-1", "sn-1", 1000L, null, "node-1");
    }

    @Test
    public void testRoutesToMatchingHandler() {
        ProtocolEventHandler gb = Mockito.mock(ProtocolEventHandler.class);
        Mockito.when(gb.protocol()).thenReturn("gb28181");
        ProtocolEventHandler onvif = Mockito.mock(ProtocolEventHandler.class);
        Mockito.when(onvif.protocol()).thenReturn("onvif");

        InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of(gb, onvif));

        DeviceEvent e = event("gb28181");
        dispatcher.dispatch(e);

        verify(gb, times(1)).handle(e);
        verify(onvif, never()).handle(Mockito.any());
        log.info("按协议路由校验通过");
    }

    @Test
    public void testUnknownProtocolDropped() {
        ProtocolEventHandler gb = Mockito.mock(ProtocolEventHandler.class);
        Mockito.when(gb.protocol()).thenReturn("gb28181");
        InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of(gb));

        // 未知协议：不抛异常、不路由到任何 handler
        assertDoesNotThrow(() -> dispatcher.dispatch(event("onvif")));
        verify(gb, never()).handle(Mockito.any());
        log.info("未知协议安全丢弃校验通过");
    }

    @Test
    public void testNullEventSafe() {
        InboundEventDispatcher dispatcher = new InboundEventDispatcher(List.of());
        assertDoesNotThrow(() -> dispatcher.dispatch(null));
        log.info("空事件安全处理校验通过");
    }
}
