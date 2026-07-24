package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCreateResultDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.ImageCollectionConfigManager;
import io.github.lunasaw.voglander.service.task.BizTaskCreateService;

@ExtendWith(MockitoExtension.class)
class ImageCollectionCreateReplayTest {

    @Mock
    private BizTaskCreateService taskCreateService;
    @Mock
    private ImageCollectionConfigManager configManager;
    @Mock
    private DeviceManager deviceManager;
    @Mock
    private DeviceChannelManager channelManager;
    @Mock
    private BizTaskManager taskManager;

    private ImageCollectionApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ImageCollectionApplicationService(taskCreateService, configManager, deviceManager,
            channelManager, new ImageProperties(), taskManager);
    }

    @Test
    void sameCanonicalReplay_shouldNotReadCurrentCameraState() {
        BizTaskDTO accepted = task("btask-replay");
        when(taskCreateService.findAcceptedReplayResult(any(TaskCreateCommand.class)))
            .thenReturn(replay(accepted));
        when(configManager.getByTaskId("btask-replay")).thenReturn(config("btask-replay"));

        BizTaskDTO result = service.create(command("same-key"));

        assertSame(accepted, result);
        verify(deviceManager, never()).getDtoByDeviceId(any());
        verify(channelManager, never()).getDtoByDeviceId(any(), any());
        verify(taskCreateService, never()).createResult(any());
        verify(configManager, never()).create(any());
    }

    @Test
    void acceptedReplay_shouldRunBeforeCurrentScheduleAndRetentionConstraints() {
        ImageProperties properties = new ImageProperties();
        properties.getCollection().setMinIntervalSeconds(300);
        service = new ImageCollectionApplicationService(taskCreateService, configManager, deviceManager,
            channelManager, properties, taskManager);
        BizTaskDTO accepted = task("btask-old-constraints");
        when(taskCreateService.findAcceptedReplayResult(any(TaskCreateCommand.class)))
            .thenReturn(replay(accepted));
        when(configManager.getByTaskId("btask-old-constraints"))
            .thenReturn(config("btask-old-constraints"));
        LocalDateTime start = LocalDateTime.of(2026, 7, 23, 10, 0);
        ImageCollectionCreateCommand command = new ImageCollectionCreateCommand(
            "collect", "SCHEDULED", "device-1", "channel-1", start, start.plusMinutes(1), 60L,
            "LEGACY_RETENTION", "USER", "owner-1", "org-1", "old-key");

        assertSame(accepted, service.create(command));

        verify(deviceManager, never()).getDtoByDeviceId(any());
        verify(channelManager, never()).getDtoByDeviceId(any(), any());
    }

    @Test
    void explicitBlankIdempotencyKey_shouldBeRejectedInsteadOfReplaced() {
        ServiceException error = assertThrows(ServiceException.class,
            () -> service.create(command("   ")));

        assertEquals(ServiceExceptionEnum.PARAM_ERROR.getCode(), error.getCode());
        verify(taskCreateService, never()).findAcceptedReplayResult(any());
        verify(taskCreateService, never()).createResult(any());
    }

    @Test
    void missingIdempotencyKey_shouldRemainCompatibleByGeneratingAnInternalKey() {
        when(taskCreateService.findAcceptedReplayResult(any(TaskCreateCommand.class))).thenReturn(null);
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(onlineDevice());
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(onlineChannel());
        BizTaskDTO accepted = task("btask-internal-key");
        when(taskCreateService.createResult(any(TaskCreateCommand.class)))
            .thenReturn(new BizTaskCreateResultDTO(true, accepted, null));

        assertSame(accepted, service.create(command(null)));

        org.mockito.ArgumentCaptor<TaskCreateCommand> acceptedCommand =
            org.mockito.ArgumentCaptor.forClass(TaskCreateCommand.class);
        verify(taskCreateService).createResult(acceptedCommand.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
            acceptedCommand.getValue().idempotencyKey().startsWith("image-create-"));
    }

    @Test
    void historicalReplayWithoutConfig_shouldFailAsConsistencyError() {
        BizTaskDTO accepted = task("btask-missing-config");
        when(taskCreateService.findAcceptedReplayResult(any(TaskCreateCommand.class)))
            .thenReturn(replay(accepted));

        assertThrows(IllegalStateException.class, () -> service.create(command("missing-config")));

        verify(configManager, never()).create(any());
    }

    @Test
    void concurrentWinnerCommittedBeforeOfflineFailure_shouldReplayWinner() {
        BizTaskDTO accepted = task("btask-concurrent");
        when(taskCreateService.findAcceptedReplayResult(any(TaskCreateCommand.class)))
            .thenReturn(null, replay(accepted));
        DeviceDTO offline = new DeviceDTO();
        offline.setDeviceId("device-1");
        offline.setStatus(DeviceConstant.Status.OFFLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO();
        channel.setDeviceId("device-1");
        channel.setChannelId("channel-1");
        channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(offline);
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(channel);
        when(configManager.getByTaskId("btask-concurrent")).thenReturn(config("btask-concurrent"));

        assertSame(accepted, service.create(command("race-key")));

        verify(taskCreateService, never()).createResult(any());
    }

    @Test
    void newIdentityWithOfflineCamera_shouldKeepCurrentStateValidation() {
        when(taskCreateService.findAcceptedReplayResult(any(TaskCreateCommand.class))).thenReturn(null);
        DeviceDTO offline = new DeviceDTO();
        offline.setDeviceId("device-1");
        offline.setStatus(DeviceConstant.Status.OFFLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO();
        channel.setDeviceId("device-1");
        channel.setChannelId("channel-1");
        channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(offline);
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(channel);

        ServiceException error = assertThrows(ServiceException.class,
            () -> service.create(command("new-offline-key")));

        assertEquals(ServiceExceptionEnum.IMAGE_CAMERA_OFFLINE.getCode(), error.getCode());
        verify(taskCreateService, never()).createResult(any());
    }

    @Test
    void lostTaskRace_shouldRequireWinnerConfigInsteadOfRebuildingIt() {
        DeviceDTO device = onlineDevice();
        DeviceChannelDTO channel = onlineChannel();
        BizTaskDTO accepted = task("btask-race-winner");
        when(taskCreateService.findAcceptedReplayResult(any(TaskCreateCommand.class))).thenReturn(null);
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(channel);
        when(taskCreateService.createResult(any(TaskCreateCommand.class))).thenReturn(replay(accepted));
        when(configManager.getByTaskId("btask-race-winner")).thenReturn(config("btask-race-winner"));

        assertSame(accepted, service.create(command("race-winner-key")));

        verify(configManager, never()).create(any());
    }

    private static ImageCollectionCreateCommand command(String key) {
        return new ImageCollectionCreateCommand("collect", "ONCE", "device-1", "channel-1",
            null, null, null, "PERMANENT", "USER", "owner-1", "org-1", key);
    }

    private static BizTaskDTO task(String taskId) {
        BizTaskDTO task = new BizTaskDTO();
        task.setTaskId(taskId);
        task.setTaskType("IMAGE_COLLECTION");
        task.setTaskMode("ONCE");
        return task;
    }

    private static BizTaskCreateResultDTO replay(BizTaskDTO task) {
        return new BizTaskCreateResultDTO(false, task, null);
    }

    private static ImageCollectionConfigDTO config(String taskId) {
        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId(taskId);
        config.setDeviceId("device-1");
        config.setChannelId("channel-1");
        return config;
    }

    private static DeviceDTO onlineDevice() {
        DeviceDTO device = new DeviceDTO();
        device.setDeviceId("device-1");
        device.setStatus(DeviceConstant.Status.ONLINE);
        return device;
    }

    private static DeviceChannelDTO onlineChannel() {
        DeviceChannelDTO channel = new DeviceChannelDTO();
        channel.setDeviceId("device-1");
        channel.setChannelId("channel-1");
        channel.setStatus(DeviceConstant.Status.ONLINE);
        return channel;
    }
}
