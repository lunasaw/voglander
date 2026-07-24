package io.github.lunasaw.voglander.repository.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.repository.domain.image.ImageCollectionTaskQueryCondition;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionTaskDO;

class ImageCollectionTaskMapperIntegrationTest extends BaseTest {

    @Autowired private BizTaskMapper taskMapper;
    @Autowired private ImageCollectionConfigMapper configMapper;
    @Autowired private ImageCollectionTaskReadMapper readMapper;

    @Test
    void cameraFilter_shouldRunBeforePaginationAndKeepCountConsistent() {
        String suffix = Long.toHexString(System.nanoTime());
        LocalDateTime base = LocalDateTime.now().minusHours(1).withNano(0);
        for (int index = 0; index < 21; index++) {
            String taskId = "btask-join-" + suffix + "-" + index;
            taskMapper.insert(task(taskId, "owner-" + suffix, base.plusSeconds(index), "IMAGE_COLLECTION"));
            configMapper.insert(config(taskId, "device-other-" + suffix, "channel-other"));
        }
        String matchingTaskId = "btask-join-" + suffix + "-matching";
        taskMapper.insert(task(matchingTaskId, "owner-" + suffix, base.minusSeconds(1), "IMAGE_COLLECTION"));
        configMapper.insert(config(matchingTaskId, "device-target-" + suffix, "channel-target"));

        ImageCollectionTaskQueryCondition condition = new ImageCollectionTaskQueryCondition();
        condition.setDeviceId("device-target-" + suffix);
        condition.setChannelId("channel-target");
        condition.setGlobal(true);
        condition.setAllowedTaskTypes(Collections.singleton("IMAGE_COLLECTION"));
        Page<ImageCollectionTaskDO> result = readMapper.selectPageByCondition(
            new Page<ImageCollectionTaskDO>(1, 20), condition);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(matchingTaskId, result.getRecords().get(0).getTask().getTaskId());
        assertEquals("channel-target", result.getRecords().get(0).getConfig().getChannelId());
    }

    @Test
    void joinedQuery_shouldEnforceOwnerAndTaskTypeScope() {
        String suffix = Long.toHexString(System.nanoTime());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BizTaskDO visible = task("btask-visible-" + suffix, "owner-a-" + suffix, now, "IMAGE_COLLECTION");
        taskMapper.insert(visible);
        configMapper.insert(config(visible.getTaskId(), "device-1", "channel-1"));
        BizTaskDO hidden = task("btask-hidden-" + suffix, "owner-b-" + suffix, now.plusSeconds(1), "IMAGE_COLLECTION");
        taskMapper.insert(hidden);
        configMapper.insert(config(hidden.getTaskId(), "device-1", "channel-1"));
        BizTaskDO wrongType = task("btask-type-" + suffix, "owner-a-" + suffix, now.plusSeconds(2), "EXPORT");
        taskMapper.insert(wrongType);
        configMapper.insert(config(wrongType.getTaskId(), "device-1", "channel-1"));

        ImageCollectionTaskQueryCondition condition = new ImageCollectionTaskQueryCondition();
        condition.setGlobal(false);
        condition.setOwnerType("USER");
        condition.setOwnerId("owner-a-" + suffix);
        condition.setAllowedTaskTypes(Collections.singleton("IMAGE_COLLECTION"));
        Page<ImageCollectionTaskDO> result = readMapper.selectPageByCondition(
            new Page<ImageCollectionTaskDO>(1, 20), condition);

        assertEquals(1L, result.getTotal());
        assertEquals(visible.getTaskId(), result.getRecords().get(0).getTask().getTaskId());
    }

    private static BizTaskDO task(String taskId, String ownerId, LocalDateTime createTime, String type) {
        BizTaskDO task = new BizTaskDO();
        task.setCreateTime(createTime);
        task.setUpdateTime(createTime);
        task.setTaskId(taskId);
        task.setTaskType(type);
        task.setTaskName(taskId);
        task.setTaskMode("ONCE");
        task.setScheduleVersion(1);
        task.setState("SCHEDULED");
        task.setPriority(0);
        task.setPlannedCount(1);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setMissedCount(0);
        task.setCancelledCount(0);
        task.setProgressCurrent(0L);
        task.setProgressTotal(0L);
        task.setProgressRevision(0L);
        task.setPayload("{}");
        task.setPayloadVersion(1);
        task.setOwnerType("USER");
        task.setOwnerId(ownerId);
        task.setVersion(0);
        return task;
    }

    private static ImageCollectionConfigDO config(String taskId, String deviceId, String channelId) {
        ImageCollectionConfigDO config = new ImageCollectionConfigDO();
        config.setCreateTime(LocalDateTime.now().withNano(0));
        config.setUpdateTime(config.getCreateTime());
        config.setTaskId(taskId);
        config.setDeviceId(deviceId);
        config.setChannelId(channelId);
        config.setRetentionPolicy("PERMANENT");
        config.setVersion(0);
        return config;
    }
}
