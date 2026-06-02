package io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.sipgateway.core.api.envelope.GatewayEvent;
import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import io.github.lunasaw.voglander.manager.event.ShardDispatcher;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 4：VoglanderBusinessNotifier 分片版本单元测试。
 * <p>
 * 校验 {@code notify(GatewayEvent)} 翻译后提交到 {@link ShardDispatcher}，
 * 而非直接调用 InboundEventDispatcher。
 * </p>
 *
 * @author luna
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class VoglanderBusinessNotifierShardTest {

    @Mock
    private ShardDispatcher           shardDispatcher;

    @InjectMocks
    private VoglanderBusinessNotifier notifier;

    @Test
    public void testDispatchesToShardDispatcher() {
        Map<String, Object> payload = Map.of("expire", 3600);
        GatewayEvent ge = new GatewayEvent("gb28181.Lifecycle.Register", "dev-9", "sn-9", 12345L, payload, "node-A");

        notifier.notify(ge);

        verify(shardDispatcher, times(1)).dispatch(any(DeviceEvent.class));
        log.info("GatewayEvent→DeviceEvent 翻译并提交到 ShardDispatcher 通过");
    }

    @Test
    public void testNullEventIgnored() {
        notifier.notify(null);
        verify(shardDispatcher, never()).dispatch(any());
        log.info("空事件忽略校验通过（分片版本）");
    }

    @Test
    public void testNonThreeSegmentTypeIgnored() {
        GatewayEvent ge = new GatewayEvent("badtype", "dev-1", "sn-1", 1L, null, "node-A");
        notifier.notify(ge);
        verify(shardDispatcher, never()).dispatch(any());
        log.info("非三段式 type 忽略校验通过（分片版本）");
    }
}
