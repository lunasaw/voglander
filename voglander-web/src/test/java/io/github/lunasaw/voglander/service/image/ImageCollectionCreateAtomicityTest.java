package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.github.lunasaw.voglander.BaseAsyncTest;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.manager.ImageCollectionConfigManager;
import io.github.lunasaw.voglander.manager.service.BizTaskEventService;
import io.github.lunasaw.voglander.manager.service.BizTaskExecutionService;
import io.github.lunasaw.voglander.manager.service.BizTaskService;
import io.github.lunasaw.voglander.manager.service.DeviceChannelService;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;
import io.github.lunasaw.voglander.repository.entity.DeviceChannelDO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;

class ImageCollectionCreateAtomicityTest extends BaseAsyncTest {

    @Autowired
    private ImageCollectionApplicationService applicationService;
    @Autowired
    private BizTaskService taskService;
    @Autowired
    private BizTaskExecutionService executionService;
    @Autowired
    private BizTaskEventService eventService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceChannelService channelService;

    @MockitoBean
    private ImageCollectionConfigManager configManager;

    private final AtomicReference<String> attemptedTaskId = new AtomicReference<String>();
    private String ownerId;
    private String deviceId;
    private String channelId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        ownerId = "collection-atomic-owner-" + suffix;
        deviceId = "collection-atomic-device-" + suffix;
        channelId = "collection-atomic-channel-" + suffix;
        LocalDateTime now = LocalDateTime.now();

        DeviceDO device = new DeviceDO();
        device.setCreateTime(now);
        device.setUpdateTime(now);
        device.setDeviceId(deviceId);
        device.setType(1);
        device.setStatus(1);
        device.setName("Atomic camera");
        device.setIp("127.0.0.1");
        device.setPort(5060);
        device.setRegisterTime(now);
        device.setKeepaliveTime(now);
        device.setServerIp("127.0.0.1");
        deviceService.save(device);

        DeviceChannelDO channel = new DeviceChannelDO();
        channel.setCreateTime(now);
        channel.setUpdateTime(now);
        channel.setStatus(1);
        channel.setChannelId(channelId);
        channel.setDeviceId(deviceId);
        channel.setName("Atomic channel");
        channel.setMissingCount(0);
        channelService.save(channel);

        doAnswer(invocation -> {
            ImageCollectionConfigDTO config = invocation.getArgument(0);
            attemptedTaskId.set(config.getTaskId());
            throw new IllegalStateException("injected config failure");
        }).when(configManager).create(any(ImageCollectionConfigDTO.class));
    }

    @AfterEach
    void tearDown() {
        List<BizTaskDO> tasks = taskService.list(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getOwnerType, "USER")
            .eq(BizTaskDO::getOwnerId, ownerId));
        List<String> taskIds = new ArrayList<String>();
        for (BizTaskDO task : tasks) {
            taskIds.add(task.getTaskId());
        }
        if (attemptedTaskId.get() != null && !taskIds.contains(attemptedTaskId.get())) {
            taskIds.add(attemptedTaskId.get());
        }
        if (!taskIds.isEmpty()) {
            eventService.remove(new LambdaQueryWrapper<BizTaskEventDO>()
                .in(BizTaskEventDO::getTaskId, taskIds));
            executionService.remove(new LambdaQueryWrapper<BizTaskExecutionDO>()
                .in(BizTaskExecutionDO::getTaskId, taskIds));
            taskService.remove(new LambdaQueryWrapper<BizTaskDO>().in(BizTaskDO::getTaskId, taskIds));
        }
        channelService.remove(new LambdaQueryWrapper<DeviceChannelDO>()
            .eq(DeviceChannelDO::getDeviceId, deviceId));
        deviceService.remove(new LambdaQueryWrapper<DeviceDO>().eq(DeviceDO::getDeviceId, deviceId));
    }

    @Test
    void configFailureRollsBackNewTaskAndFirstExecution() {
        ImageCollectionCreateCommand command = new ImageCollectionCreateCommand("atomic-create", "ONCE",
            deviceId, channelId, null, null, null, "PERMANENT", "USER", ownerId, null,
            "atomic-key-" + UUID.randomUUID().toString().replace("-", ""));

        assertThrows(IllegalStateException.class, () -> applicationService.create(command));

        assertNotNull(attemptedTaskId.get());
        assertEquals(0L, taskService.count(new LambdaQueryWrapper<BizTaskDO>()
            .eq(BizTaskDO::getTaskId, attemptedTaskId.get())));
        assertEquals(0L, executionService.count(new LambdaQueryWrapper<BizTaskExecutionDO>()
            .eq(BizTaskExecutionDO::getTaskId, attemptedTaskId.get())));
    }
}
