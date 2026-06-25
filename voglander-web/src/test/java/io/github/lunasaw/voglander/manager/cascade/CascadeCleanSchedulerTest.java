package io.github.lunasaw.voglander.manager.cascade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeRecordRequestScheduler;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.cascade.CascadeSubscribeCleanScheduler;
import io.github.lunasaw.voglander.manager.manager.CascadeRecordRequestManager;
import io.github.lunasaw.voglander.manager.manager.CascadeSubscribeManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("级联清理调度器单元测试")
class CascadeCleanSchedulerTest {

    @Mock
    CascadeRecordRequestManager      recordRequestManager;
    @Mock
    CascadeSubscribeManager          subscribeManager;
    @InjectMocks
    CascadeRecordRequestScheduler    recordScheduler;
    @InjectMocks
    CascadeSubscribeCleanScheduler   subscribeScheduler;

    @Test
    @DisplayName("录像请求调度：委托 cleanTimeout(cutoff)")
    void record_scheduler_delegates_clean_timeout() {
        when(recordRequestManager.cleanTimeout(any(LocalDateTime.class))).thenReturn(2);
        recordScheduler.cleanTimeoutRequests();
        verify(recordRequestManager).cleanTimeout(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("录像请求调度：manager 抛异常不冒泡")
    void record_scheduler_swallows_exception() {
        when(recordRequestManager.cleanTimeout(any(LocalDateTime.class))).thenThrow(new RuntimeException("db"));
        recordScheduler.cleanTimeoutRequests();
        verify(recordRequestManager).cleanTimeout(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("订阅清理调度：委托 cleanExpired(now)")
    void subscribe_scheduler_delegates_clean_expired() {
        when(subscribeManager.cleanExpired(any(LocalDateTime.class))).thenReturn(1);
        subscribeScheduler.cleanExpiredSubscribes();
        verify(subscribeManager).cleanExpired(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("订阅清理调度：manager 抛异常不冒泡")
    void subscribe_scheduler_swallows_exception() {
        when(subscribeManager.cleanExpired(any(LocalDateTime.class))).thenThrow(new RuntimeException("db"));
        subscribeScheduler.cleanExpiredSubscribes();
        verify(subscribeManager).cleanExpired(any(LocalDateTime.class));
    }
}
