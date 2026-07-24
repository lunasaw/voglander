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
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionConfigMapper;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionTaskReadMapper;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionTaskDO;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;

class ImageCollectionConfigManagerTest {
    @Mock private ImageCollectionConfigMapper configMapper;
    @Mock private ImageCollectionTaskReadMapper taskReadMapper;
    private ImageCollectionConfigManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); manager = new ImageCollectionConfigManager();
        ReflectionTestUtils.setField(manager, "configMapper", configMapper);
        ReflectionTestUtils.setField(manager, "taskReadMapper", taskReadMapper);
        ReflectionTestUtils.setField(manager, "taskAssembler", new BizTaskAssembler());
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
        ImageCollectionConfigDO config = new ImageCollectionConfigDO(); config.setTaskId("btask_1"); config.setDeviceId("d1"); config.setChannelId("c1");
        ImageCollectionTaskDO projection = new ImageCollectionTaskDO();
        projection.setTask(new BizTaskAssembler().dtoToDo(task));
        projection.setConfig(config);
        when(taskReadMapper.selectDetailByCondition(any())).thenReturn(projection);

        assertEquals("RUNNING", manager.getEnrichedDetail("btask_1", null).getTask().getState());
        assertEquals("d1", manager.getEnrichedDetail("btask_1", null).getConfig().getDeviceId());
    }

    private static ImageCollectionConfigDTO config(String taskId) { ImageCollectionConfigDTO config = new ImageCollectionConfigDTO(); config.setTaskId(taskId); config.setDeviceId("d1"); config.setChannelId("c1"); return config; }
}
