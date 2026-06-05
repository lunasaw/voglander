package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link LabKeepaliveScheduler} 纯单元测试：注入 mock executor，验证周期心跳调度的开关/间隔/重启语义，
 * 不依赖真实时间。
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class LabKeepaliveSchedulerTest {

    @Mock
    private LabSipClient            labSipClient;

    @Mock
    private ScheduledExecutorService executor;

    @SuppressWarnings("rawtypes")
    @Mock
    private ScheduledFuture           future;

    private LabKeepaliveScheduler newScheduler() {
        return new LabKeepaliveScheduler(labSipClient, executor);
    }

    @Test
    public void testEnableSchedulesAtGivenInterval() {
        when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any())).thenReturn(future);
        LabKeepaliveScheduler scheduler = newScheduler();

        scheduler.setAuto(true, 10);

        verify(executor, times(1)).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(10L), eq(TimeUnit.SECONDS));
        assertTrue(scheduler.isEnabled());
        assertEquals(10, scheduler.getIntervalSec());
        log.info("启用周期心跳：按指定间隔调度 校验通过");
    }

    @Test
    public void testScheduledTaskInvokesKeepalive() {
        when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any())).thenReturn(future);
        LabKeepaliveScheduler scheduler = newScheduler();

        scheduler.setAuto(true, 5);

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(taskCaptor.capture(), anyLong(), anyLong(), any());
        taskCaptor.getValue().run();
        verify(labSipClient, times(1)).keepalive();
        log.info("调度任务体触发 LabSipClient.keepalive() 校验通过");
    }

    @Test
    public void testNonPositiveIntervalFallsBackToDefault() {
        when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any())).thenReturn(future);
        LabKeepaliveScheduler scheduler = newScheduler();

        scheduler.setAuto(true, 0);

        verify(executor).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(30L), eq(TimeUnit.SECONDS));
        assertEquals(30, scheduler.getIntervalSec());
        log.info("非正间隔回落默认 30s 校验通过");
    }

    @Test
    public void testReEnableCancelsPreviousTask() {
        when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any())).thenReturn(future);
        LabKeepaliveScheduler scheduler = newScheduler();

        scheduler.setAuto(true, 10);
        scheduler.setAuto(true, 20);

        // 旧任务被取消，新任务以 20s 调度
        verify(future, times(1)).cancel(false);
        verify(executor).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(20L), eq(TimeUnit.SECONDS));
        assertEquals(20, scheduler.getIntervalSec());
        log.info("重复启用先取消旧任务 校验通过");
    }

    @Test
    public void testDisableCancelsTaskAndClearsState() {
        when(executor.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any())).thenReturn(future);
        LabKeepaliveScheduler scheduler = newScheduler();
        scheduler.setAuto(true, 10);

        scheduler.setAuto(false, 0);

        verify(future, times(1)).cancel(false);
        assertFalse(scheduler.isEnabled());
        assertEquals(0, scheduler.getIntervalSec());
        log.info("关闭周期心跳：取消任务并清状态 校验通过");
    }

    @Test
    public void testDisableWhenNotRunningIsNoop() {
        LabKeepaliveScheduler scheduler = newScheduler();

        scheduler.setAuto(false, 0);

        verifyNoInteractions(executor);
        assertFalse(scheduler.isEnabled());
        log.info("未运行时关闭为空操作 校验通过");
    }
}
