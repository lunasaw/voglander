package io.github.lunasaw.voglander.service.sse;

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

import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * SseRelayListener 单元测试（纯 Mockito）。
 * <p>
 * 验证 common 层 {@link SseRelayEvent} 被转换为 {@link SseEvent} 并投递到 {@link SseEventBus}，
 * topic/data 原样透传；topic 为 null 时不投递。
 * </p>
 *
 * @author luna
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class SseRelayListenerTest {

    @Mock
    private SseEventBus      sseEventBus;

    @InjectMocks
    private SseRelayListener listener;

    @Test
    public void testRelayPublishesSseEventWithSameTopicAndData() {
        Map<String, Object> data = Map.of("deviceId", "34020000001320000001", "ts", 1733000000000L);

        listener.onSseRelay(new SseRelayEvent("device.register", data));

        ArgumentCaptor<SseEvent> captor = ArgumentCaptor.forClass(SseEvent.class);
        verify(sseEventBus, times(1)).publish(captor.capture());
        assertEquals("device.register", captor.getValue().getTopic(), "topic 应透传");
        assertEquals(data, captor.getValue().getData(), "data 应透传");
        log.info("SseRelayEvent→SseEventBus.publish 透传校验通���");
    }

    @Test
    public void testNullTopicIsNoop() {
        listener.onSseRelay(new SseRelayEvent(null, Map.of("k", "v")));
        verify(sseEventBus, never()).publish(any());
        log.info("topic=null 不投递校验通过");
    }
}
