package io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.manager.event.InboundEventDispatcher;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 3：VoglanderBusinessNotifierFallback 翻译器单元测试（PROTOCOL S2 回退版本）。
 * <p>
 * 校验 {@code notify(GatewayEvent)} 仅做 {@code GatewayEvent → DeviceEvent} 翻译并交给
 * {@link InboundEventDispatcher}，三段式 type 正确切分，字段透传无损，非三段式/空事件安全忽略。
 * </p>
 *
 * @author luna
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class VoglanderBusinessNotifierTranslatorTest {

    @Mock
    private InboundEventDispatcher          dispatcher;

    @InjectMocks
    private VoglanderBusinessNotifierFallback notifier;

    @Test
    public void testTranslatesGatewayEventToDeviceEvent() {
        Map<String, Object> payload = Map.of("expire", 3600);
        GatewayEvent ge = new GatewayEvent("gb28181.Lifecycle.Register", "dev-9", "sn-9", 12345L, payload, "node-A");

        notifier.notify(ge);

        ArgumentCaptor<DeviceEvent> captor = ArgumentCaptor.forClass(DeviceEvent.class);
        verify(dispatcher, times(1)).dispatch(captor.capture());
        DeviceEvent de = captor.getValue();

        assertEquals("gb28181", de.protocol(), "protocol 段");
        assertEquals("Lifecycle", de.group(), "group 段");
        assertEquals("Register", de.name(), "name 段");
        assertEquals("dev-9", de.deviceId());
        assertEquals("sn-9", de.correlationId());
        assertEquals(12345L, de.timestampMs());
        assertEquals("node-A", de.nodeId());
        assertEquals(payload, de.payload(), "payload 透传无损（仍是原始 Map）");
        assertEquals("gb28181.Lifecycle.Register", de.type());
        log.info("GatewayEvent→DeviceEvent 翻译校验通过（Fallback 版本）");
    }

    @Test
    public void testNullEventIgnored() {
        notifier.notify(null);
        verify(dispatcher, never()).dispatch(any());
        log.info("空事件忽略校验通过");
    }

    @Test
    public void testNonThreeSegmentTypeIgnored() {
        GatewayEvent ge = new GatewayEvent("badtype", "dev-1", "sn-1", 1L, null, "node-A");
        notifier.notify(ge);
        verify(dispatcher, never()).dispatch(any());
        log.info("非三段式 type 忽略校验通过");
    }
}
