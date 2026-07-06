package io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterFailureEvent;
import io.github.lunasaw.gbproxy.client.eventbus.event.ClientRegisterSuccessEvent;
import io.github.lunasaw.voglander.common.constant.cascade.CascadeConstant;
import io.github.lunasaw.voglander.common.event.SseRelayEvent;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.manager.CascadePlatformManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("CascadeClientRegisterListener 单元测试")
class CascadeClientRegisterListenerTest {

    private static final String PLATFORM_ID = "44010000002000000001";
    private static final String LOCAL_ID    = "34020000001320000001";

    @Mock
    private CascadePlatformManager    cascadePlatformManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CascadeClientRegisterListener listener;

    @BeforeEach
    void setUp() {
        listener = new CascadeClientRegisterListener(cascadePlatformManager, eventPublisher);
    }

    private CascadePlatformDTO platform() {
        CascadePlatformDTO dto = new CascadePlatformDTO();
        dto.setId(7L);
        dto.setPlatformId(PLATFORM_ID);
        dto.setLocalClientId(LOCAL_ID);
        return dto;
    }

    @Test
    @DisplayName("注册成功事件 userId=platformId 时应更新 ONLINE")
    void successEvent_shouldResolveByPlatformId() {
        when(cascadePlatformManager.getByPlatformId(PLATFORM_ID)).thenReturn(platform());

        listener.onRegisterSuccess(new ClientRegisterSuccessEvent(this, PLATFORM_ID));

        verify(cascadePlatformManager).updateRegisterStatus(7L, CascadeConstant.RegisterStatus.ONLINE);
        verify(cascadePlatformManager, never()).getByLocalClientId(any());
        verify(eventPublisher).publishEvent(any(SseRelayEvent.class));
    }

    @Test
    @DisplayName("注册失败事件 userId=localClientId 时兼容旧路径并更新 FAILED")
    void failureEvent_shouldFallbackToLocalClientId() {
        when(cascadePlatformManager.getByPlatformId(LOCAL_ID)).thenReturn(null);
        when(cascadePlatformManager.getByLocalClientId(LOCAL_ID)).thenReturn(platform());

        listener.onRegisterFailure(new ClientRegisterFailureEvent(this, LOCAL_ID, 403));

        verify(cascadePlatformManager).updateRegisterStatus(7L, CascadeConstant.RegisterStatus.FAILED);
        verify(eventPublisher).publishEvent(any(SseRelayEvent.class));
    }
}
