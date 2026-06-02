package io.github.lunasaw.voglander.service.stream;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.service.StreamProxyService;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.service.stream.impl.StreamProxyBizServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 0：StreamProxy 定时同步任务开关单元测试
 * <p>
 * 启用 {@code @EnableScheduling} 后，原本从未运行过的 {@code syncAllEnabledStreamProxyStatus}
 * （30s 周期、轮询 ZLM + 写库）开始执行。本测试校验：
 * </p>
 * <ul>
 * <li>定时触发器 {@code scheduledSyncTask()} 受开关 {@code scheduledSyncEnabled} 控制；</li>
 * <li>开关关闭时定时触发器直接返回，不触碰任何数据访问层（无副作用）；</li>
 * <li>开关开启时定时触发器才执行实际同步逻辑（命中分页查询）；</li>
 * <li>开关只 gate 定时触发器，不影响接口方法 {@code syncAllEnabledStreamProxyStatus()} 被手动直接调用。</li>
 * </ul>
 *
 * @author luna
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class StreamProxyScheduledSyncTest {

    @Mock
    private StreamProxyService          streamProxyService;

    @InjectMocks
    private StreamProxyBizServiceImpl   streamProxyBizService;

    @Test
    public void testScheduledTaskSkippedWhenDisabled() {
        // Given：开关关闭
        ReflectionTestUtils.setField(streamProxyBizService, "scheduledSyncEnabled", false);

        // When：定时触发器被调用
        streamProxyBizService.scheduledSyncTask();

        // Then：不触碰数据访问层，无任何副作用
        verifyNoInteractions(streamProxyService);
        log.info("开关关闭时定时同步被跳过，无副作用 - 校验通过");
    }

    @Test
    public void testScheduledTaskRunsWhenEnabled() {
        // Given：开关开启，分页查询返回空页（立即结束循环）
        ReflectionTestUtils.setField(streamProxyBizService, "scheduledSyncEnabled", true);
        Page<StreamProxyDO> emptyPage = new Page<>(1, 100);
        emptyPage.setRecords(java.util.Collections.emptyList());
        when(streamProxyService.page(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(emptyPage);

        // When：定时触发器被调用
        streamProxyBizService.scheduledSyncTask();

        // Then：执行实际同步逻辑，命中分页查询
        verify(streamProxyService, times(1)).page(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        log.info("开关开启时定时同步正常执行 - 校验通过");
    }

    @Test
    public void testManualSyncAlwaysRunsRegardlessOfSwitch() {
        // Given：开关关闭（不应影响手动直接调用接口方法）
        ReflectionTestUtils.setField(streamProxyBizService, "scheduledSyncEnabled", false);
        Page<StreamProxyDO> emptyPage = new Page<>(1, 100);
        emptyPage.setRecords(java.util.Collections.emptyList());
        when(streamProxyService.page(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(emptyPage);

        // When：手动直接调用接口方法
        int count = streamProxyBizService.syncAllEnabledStreamProxyStatus();

        // Then：照常执行（开关只 gate 定时触发器，不 gate 手动调用）
        verify(streamProxyService, times(1)).page(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        log.info("手动调用不受开关影响，同步记录数: {} - 校验通过", count);
    }
}
