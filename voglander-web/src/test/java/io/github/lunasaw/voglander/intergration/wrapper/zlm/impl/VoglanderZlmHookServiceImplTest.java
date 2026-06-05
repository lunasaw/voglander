package io.github.lunasaw.voglander.intergration.wrapper.zlm.impl;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.voglander.common.event.StreamReadyEvent;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.auth.ZlmHookAuthService;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.zlm.hook.param.OnStreamChangedHookParam;

/**
 * VoglanderZlmHookServiceImpl 单测：流上线发布 StreamReadyEvent，流下线不发布。
 */
@ExtendWith(MockitoExtension.class)
class VoglanderZlmHookServiceImplTest {

    @InjectMocks
    VoglanderZlmHookServiceImpl hookService;

    @Mock
    MediaNodeManager             mediaNodeManager;
    @Mock
    StreamProxyManager           streamProxyManager;
    @Mock
    ZlmHookAuthService           zlmHookAuthService;
    @Mock
    ApplicationEventPublisher    eventPublisher;

    @Test
    void testOnStreamChanged_Regist_PublishesStreamReadyEvent() {
        OnStreamChangedHookParam param = new OnStreamChangedHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");
        param.setRegist(true);

        // Manager returns null (no existing proxy), that's fine — event still fires
        when(streamProxyManager.get(any())).thenReturn(null);

        hookService.onStreamChanged(param, null);

        verify(eventPublisher, times(1)).publishEvent(any(StreamReadyEvent.class));
    }

    @Test
    void testOnStreamChanged_Unregist_NoStreamReadyEvent() {
        OnStreamChangedHookParam param = new OnStreamChangedHookParam();
        param.setApp("rtp");
        param.setStream("gb_live_dev1_ch1");
        param.setRegist(false);

        when(streamProxyManager.get(any())).thenReturn(null);

        hookService.onStreamChanged(param, null);

        verify(eventPublisher, never()).publishEvent(any(StreamReadyEvent.class));
    }
}
