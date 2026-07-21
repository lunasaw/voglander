package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.service.ImageCollectionConfigService;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionConfigMapper;

class ImageCollectionConfigManagerTest {
    @Mock private ImageCollectionConfigService configService;
    @Mock private ImageCollectionConfigMapper configMapper;
    @Mock private BizTaskManager taskManager;
    private ImageCollectionConfigManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); manager = new ImageCollectionConfigManager();
        ReflectionTestUtils.setField(manager, "configService", configService);
        ReflectionTestUtils.setField(manager, "configMapper", configMapper);
        ReflectionTestUtils.setField(manager, "taskManager", taskManager);
    }

    @Test
    void createIsIdempotentByTaskId() {
        ImageCollectionConfigDO existing = new ImageCollectionConfigDO(); existing.setTaskId("btask_1"); existing.setDeviceId("d1"); existing.setChannelId("c1");
        when(configMapper.selectByTaskId("btask_1")).thenReturn(existing);
        ImageCollectionConfigDTO input = config("btask_1");
        assertEquals("btask_1", manager.create(input).getTaskId());
    }

    @Test
    void enrichedDetailUsesGenericTaskAndConfigWithoutShadowState() {
        BizTaskDTO task = new BizTaskDTO(); task.setTaskId("btask_1"); task.setState("RUNNING");
        Page<BizTaskDTO> tasks = new Page<>(1, 1, 1); tasks.setRecords(Collections.singletonList(task));
        when(taskManager.getPage(any(), any(), any(Integer.class), any(Integer.class))).thenReturn(tasks);
        ImageCollectionConfigDO config = new ImageCollectionConfigDO(); config.setTaskId("btask_1"); config.setDeviceId("d1"); config.setChannelId("c1");
        when(configMapper.selectByTaskId("btask_1")).thenReturn(config);

        assertEquals("RUNNING", manager.getEnrichedDetail("btask_1", null).getTask().getState());
        assertEquals("d1", manager.getEnrichedDetail("btask_1", null).getConfig().getDeviceId());
    }

    private static ImageCollectionConfigDTO config(String taskId) { ImageCollectionConfigDTO config = new ImageCollectionConfigDTO(); config.setTaskId(taskId); config.setDeviceId("d1"); config.setChannelId("c1"); return config; }
}
