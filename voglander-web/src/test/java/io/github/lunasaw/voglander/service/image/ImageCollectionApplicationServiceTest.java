package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCommandDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.ImageCollectionConfigManager;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.service.task.BizTaskCreateService;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;

class ImageCollectionApplicationServiceTest {
    @Mock private BizTaskCreateService taskCreateService;
    @Mock private ImageCollectionConfigManager configManager;
    @Mock private DeviceManager deviceManager;
    @Mock private DeviceChannelManager channelManager;
    @Mock private BizTaskManager taskManager;
    private ImageCollectionApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ImageCollectionApplicationService(taskCreateService, configManager, deviceManager,
            channelManager, new ImageProperties(), taskManager);
    }

    @Test
    void onceCreatesGenericTaskAndConfigWithoutScheduleFields() {
        DeviceDTO device = new DeviceDTO();
        device.setDeviceId("d1"); device.setName("Gate"); device.setStatus(DeviceConstant.Status.ONLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO();
        channel.setDeviceId("d1"); channel.setChannelId("c1"); channel.setName("Main");
        channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("d1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("d1", "c1")).thenReturn(channel);
        BizTaskDTO task = new BizTaskDTO(); task.setTaskId("btask_1"); task.setTaskMode("ONCE");
        when(taskCreateService.create(any())).thenReturn(task);

        BizTaskDTO result = service.create(new ImageCollectionCreateCommand("one", "ONCE", "d1", "c1",
            null, null, null, "PERMANENT", "USER", "7", null, "key-1"));

        assertEquals("btask_1", result.getTaskId());
        verify(configManager).create(any(ImageCollectionConfigDTO.class));
    }

    @Test
    void scheduledRejectsIntervalBelowImageConstraintBeforeTaskCreation() {
        DeviceDTO device = new DeviceDTO(); device.setDeviceId("d1"); device.setStatus(DeviceConstant.Status.ONLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setDeviceId("d1");
        channel.setChannelId("c1"); channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("d1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("d1", "c1")).thenReturn(channel);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.create(
            new ImageCollectionCreateCommand("scheduled", "SCHEDULED", "d1", "c1", LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(5), 1L, "PERMANENT", "USER", "7", null, "key-2")));

        assertEquals(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void onceRejectsAnyScheduleField() {
        DeviceDTO device = new DeviceDTO(); device.setDeviceId("d1"); device.setStatus(DeviceConstant.Status.ONLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setDeviceId("d1");
        channel.setChannelId("c1"); channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("d1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("d1", "c1")).thenReturn(channel);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.create(
            new ImageCollectionCreateCommand("once", "ONCE", "d1", "c1", LocalDateTime.now(), null, null,
                "PERMANENT", "USER", "7", null, "key-3")));
        assertEquals(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID.getCode(), exception.getCode());
    }

    @Test
    void scheduledUsesFixedRateAndInclusivePlanFields() {
        DeviceDTO device = new DeviceDTO(); device.setDeviceId("d1"); device.setName("Gate"); device.setStatus(DeviceConstant.Status.ONLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setDeviceId("d1"); channel.setChannelId("c1");
        channel.setName("Main"); channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("d1")).thenReturn(device); when(channelManager.getDtoByDeviceId("d1", "c1")).thenReturn(channel);
        BizTaskDTO task = new BizTaskDTO(); task.setTaskId("btask_schedule"); task.setTaskMode("FIXED_RATE"); task.setPlannedCount(3);
        when(taskCreateService.create(any(TaskCreateCommand.class))).thenReturn(task);
        LocalDateTime start = LocalDateTime.of(2026, 7, 15, 10, 0); LocalDateTime end = start.plusMinutes(2);

        BizTaskDTO result = service.create(new ImageCollectionCreateCommand("scheduled", "SCHEDULED", "d1", "c1",
            start, end, 60L, "PERMANENT", "USER", "7", null, "key-scheduled"));

        assertEquals("btask_schedule", result.getTaskId());
        verify(taskCreateService).create(argThat(command -> "FIXED_RATE".equals(command.taskMode())
            && command.scheduleStartTime().equals(start) && command.scheduleEndTime().equals(end)
            && command.intervalSeconds().longValue() == 60L && command.payload().getString("deviceId").equals("d1")));
    }

    @Test
    void reschedule_shouldValidatePausedImageTaskAndDelegateToGenericManager() {
        service = new ImageCollectionApplicationService(taskCreateService, configManager, deviceManager,
            channelManager, new ImageProperties(), taskManager);
        BizTaskDTO paused = new BizTaskDTO();
        paused.setTaskId("btask-paused"); paused.setTaskType("IMAGE_COLLECTION");
        paused.setTaskMode("FIXED_RATE"); paused.setState("PAUSED"); paused.setVersion(4);
        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId("btask-paused"); config.setDeviceId("d1"); config.setChannelId("c1");
        when(taskManager.getByTaskId("btask-paused", io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO.global()))
            .thenReturn(paused);
        when(configManager.getByTaskId("btask-paused")).thenReturn(config);
        BizTaskCommandDTO command = new BizTaskCommandDTO();
        command.setTaskId("btask-paused"); command.setExpectedVersion(4);
        command.setScheduleStartTime(LocalDateTime.of(2026, 7, 15, 10, 0));
        command.setScheduleEndTime(LocalDateTime.of(2026, 7, 15, 10, 2));
        command.setIntervalSeconds(60L);
        when(taskManager.reschedule(command)).thenReturn(paused);

        assertEquals(paused, service.reschedule(command));
        verify(taskManager).reschedule(command);
    }

    @Test
    void reschedule_shouldRejectNonPausedOrCameraMutationAndInvalidConstraints() {
        service = new ImageCollectionApplicationService(taskCreateService, configManager, deviceManager,
            channelManager, new ImageProperties(), taskManager);
        BizTaskDTO running = new BizTaskDTO();
        running.setTaskId("btask-running"); running.setTaskType("IMAGE_COLLECTION");
        running.setTaskMode("FIXED_RATE"); running.setState("RUNNING");
        when(taskManager.getByTaskId("btask-running", io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO.global()))
            .thenReturn(running);
        BizTaskCommandDTO command = new BizTaskCommandDTO(); command.setTaskId("btask-running");
        command.setScheduleStartTime(LocalDateTime.now()); command.setScheduleEndTime(LocalDateTime.now().plusMinutes(1));
        command.setIntervalSeconds(60L);
        assertThrows(ServiceException.class, () -> service.reschedule(command));
        org.mockito.Mockito.verify(taskManager).getByTaskId("btask-running",
            io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO.global());
        org.mockito.Mockito.verify(taskManager, org.mockito.Mockito.never()).reschedule(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_shouldPassOwnerAndIdempotencyToGenericTask_andKeepConfigAsCameraSnapshotOnly() {
        DeviceDTO device = new DeviceDTO(); device.setDeviceId("d1"); device.setName("Gate"); device.setStatus(DeviceConstant.Status.ONLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setDeviceId("d1"); channel.setChannelId("c1");
        channel.setName("Main"); channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("d1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("d1", "c1")).thenReturn(channel);
        BizTaskDTO task = new BizTaskDTO(); task.setTaskId("btask-idempotent"); task.setTaskMode("ONCE");
        when(taskCreateService.create(any(TaskCreateCommand.class))).thenReturn(task);

        service.create(new ImageCollectionCreateCommand("one", "ONCE", "d1", "c1", null, null, null,
            "PERMANENT", "USER", "7", "org-1", "idem-1"));

        verify(taskCreateService).create(argThat(command -> "USER".equals(command.ownerType())
            && "7".equals(command.ownerId()) && "org-1".equals(command.organizationId())
            && "idem-1".equals(command.idempotencyKey())
            && command.payload().keySet().stream().noneMatch(key -> key.toLowerCase().contains("secret")
                || key.toLowerCase().contains("url") || key.toLowerCase().contains("path"))));
        verify(configManager).create(argThat(config -> "btask-idempotent".equals(config.getTaskId())
            && "d1".equals(config.getDeviceId()) && "c1".equals(config.getChannelId())
            && "Gate".equals(config.getDeviceNameSnapshot()) && "Main".equals(config.getChannelNameSnapshot())
            && config.getCaptureOptions().getString("deviceId").equals("d1")));
    }
}
