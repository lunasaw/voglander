package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.client.domain.image.ImagePromoteCommand;
import io.github.lunasaw.voglander.client.domain.image.ImageStageCommand;
import io.github.lunasaw.voglander.client.domain.image.MediaSnapshotCommand;
import io.github.lunasaw.voglander.client.domain.image.SnapshotContent;
import io.github.lunasaw.voglander.client.domain.image.StagedImage;
import io.github.lunasaw.voglander.client.domain.image.StoredImage;
import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.client.service.task.TaskCancellationToken;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.enums.image.ImageFormatEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.manager.manager.ImageCollectionConfigManager;
import io.github.lunasaw.voglander.client.service.image.MediaSnapshotAdapter;

class ImageCollectionTaskHandlerExecutionTest {
    @Mock private ImageCollectionConfigManager configManager;
    @Mock private CaptureStreamLeaseService leaseService;
    @Mock private MediaSnapshotAdapter snapshotAdapter;
    @Mock private ImageStorageService storage;
    @Mock private ImageValidationService validation;
    @Mock private ImageCollectionCompletionParticipant participant;
    @Mock private DeviceManager deviceManager;
    @Mock private DeviceChannelManager channelManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_shouldValidateCameraCaptureAndReturnSanitizedCompletionFacts() throws Exception {
        ImageProperties properties = new ImageProperties();
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler(configManager, leaseService, snapshotAdapter,
            storage, validation, properties, participant, deviceManager, channelManager);
        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId("btask-1"); config.setDeviceId("device-1"); config.setChannelId("channel-1");
        config.setDeviceNameSnapshot("North Gate Device Snapshot");
        config.setChannelNameSnapshot("North Gate Channel Snapshot");
        when(configManager.getByTaskId("btask-1")).thenReturn(config);
        DeviceDTO device = new DeviceDTO(); device.setDeviceId("device-1"); device.setStatus(DeviceConstant.Status.ONLINE);
        device.setName("Renamed Device");
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setDeviceId("device-1");
        channel.setChannelId("channel-1"); channel.setStatus(DeviceConstant.Status.ONLINE);
        channel.setName("Renamed Channel");
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(channel);

        io.github.lunasaw.voglander.service.live.MediaPlayService media = mock(io.github.lunasaw.voglander.service.live.MediaPlayService.class);
        CaptureStreamLease lease = new CaptureStreamLease(media, "stream-1", "node-1", "rtsp://camera/live");
        when(leaseService.acquire("device-1", "channel-1")).thenReturn(lease);
        when(snapshotAdapter.capture(any(MediaSnapshotCommand.class))).thenReturn(
            new SnapshotContent(new ByteArrayInputStream(new byte[] {1, 2, 3}), 3, Instant.now(), () -> { }));
        when(storage.stage(any(ImageStageCommand.class), any())).thenReturn(new StagedImage("stage-1", 3, "abc123"));
        when(storage.openStaged("stage-1")).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        when(validation.inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class)))
            .thenReturn(new VerifiedImage(ImageFormatEnum.JPEG, "image/jpeg", 3, 1, 3));
        when(storage.promote(any(ImagePromoteCommand.class))).thenReturn(new StoredImage("images/asset.jpg", 3));
        doNothing().when(storage).discardStaged("stage-1");

        JSONObject payload = new JSONObject(); payload.put("payloadVersion", 1);
        payload.put("deviceId", "device-1"); payload.put("channelId", "channel-1");
        LongTaskContext context = context("btask-1", "bexec-1");

        var result = handler.execute(context, payload);

        assertEquals("IMAGE_ASSET", result.resultReference().type());
        assertNotNull(result.resultReference().id());
        JSONObject source = result.completionData().getJSONObject("source");
        JSONObject metadata = source.getJSONObject("sourceMetadata");
        assertEquals("device-1", metadata.getString("deviceId"));
        assertEquals("channel-1", metadata.getString("channelId"));
        assertEquals("North Gate Device Snapshot", metadata.getString("deviceName"));
        assertEquals("North Gate Channel Snapshot", metadata.getString("channelName"));
        assertEquals("RTSP", metadata.getString("protocol"));
        assertEquals("node-1", metadata.getString("nodeServerId"));
        assertEquals("stream-1", metadata.getString("streamId"));
        verify(storage).promote(any(ImagePromoteCommand.class));
        verify(media).stopLive("stream-1");
        verify(storage).discardStaged("stage-1");
    }

    @Test
    void execute_shouldPollCancellationAfterStaging_andReleaseSnapshotLeaseAndStage() throws Exception {
        ImageProperties properties = new ImageProperties();
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler(configManager, leaseService, snapshotAdapter,
            storage, validation, properties, participant, deviceManager, channelManager);
        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId("btask-cancel"); config.setDeviceId("device-1"); config.setChannelId("channel-1");
        when(configManager.getByTaskId("btask-cancel")).thenReturn(config);
        DeviceDTO device = new DeviceDTO(); device.setStatus(DeviceConstant.Status.ONLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(channel);
        io.github.lunasaw.voglander.service.live.MediaPlayService media = mock(io.github.lunasaw.voglander.service.live.MediaPlayService.class);
        when(leaseService.acquire("device-1", "channel-1"))
            .thenReturn(new CaptureStreamLease(media, "stream-cancel", "node-1", "rtsp://camera/live"));
        AtomicInteger cleanup = new AtomicInteger();
        when(snapshotAdapter.capture(any(MediaSnapshotCommand.class))).thenReturn(
            new SnapshotContent(new ByteArrayInputStream(new byte[] {1}), 1, Instant.now(), cleanup::incrementAndGet));
        when(storage.stage(any(ImageStageCommand.class), any())).thenReturn(new StagedImage("stage-cancel", 1, "hash"));
        AtomicInteger polls = new AtomicInteger();
        TaskCancellationToken token = new TaskCancellationToken() {
            @Override public boolean isCancellationRequested() { return polls.get() >= 3; }
            @Override public void throwIfCancellationRequested() {
                if (polls.incrementAndGet() >= 3) throw new IllegalStateException("cancelled");
            }
        };
        LongTaskContext context = context("btask-cancel", "bexec-cancel", token);
        JSONObject payload = new JSONObject(); payload.put("payloadVersion", 1);
        payload.put("deviceId", "device-1"); payload.put("channelId", "channel-1");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> handler.execute(context, payload));
        assertEquals(1, cleanup.get());
        verify(storage).discardStaged("stage-cancel");
        verify(media).stopLive("stream-cancel");
    }

    @Test
    void execute_shouldRejectOfflineCameraBeforeAnyMediaOrStorageCall() {
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler(configManager, leaseService, snapshotAdapter,
            storage, validation, new ImageProperties(), participant, deviceManager, channelManager);
        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId("btask-offline"); config.setDeviceId("device-1"); config.setChannelId("channel-1");
        when(configManager.getByTaskId("btask-offline")).thenReturn(config);
        DeviceDTO device = new DeviceDTO(); device.setStatus(DeviceConstant.Status.OFFLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(channel);
        JSONObject payload = new JSONObject(); payload.put("payloadVersion", 1);
        payload.put("deviceId", "device-1"); payload.put("channelId", "channel-1");

        ServiceException failure = org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
            () -> handler.execute(context("btask-offline", "bexec-offline"), payload));
        assertEquals(ServiceExceptionEnum.IMAGE_CAMERA_OFFLINE.getCode(), failure.getCode());
        org.mockito.Mockito.verifyNoInteractions(leaseService, snapshotAdapter, storage);
    }

    @Test
    void execute_shouldReturnRetryableBusyFailureWhenCameraGuardIsHeld() throws Exception {
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler(configManager, leaseService, snapshotAdapter,
            storage, validation, new ImageProperties(), participant, deviceManager, channelManager);
        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId("btask-busy"); config.setDeviceId("device-1"); config.setChannelId("channel-1");
        when(configManager.getByTaskId("btask-busy")).thenReturn(config);
        DeviceDTO device = new DeviceDTO(); device.setStatus(DeviceConstant.Status.ONLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(channel);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Semaphore> guards = (ConcurrentHashMap<String, Semaphore>) cameraGuardsField().get(null);
        Semaphore held = guards.computeIfAbsent("device-1:channel-1", ignored -> new Semaphore(1));
        assertEquals(true, held.tryAcquire());
        try {
            JSONObject payload = new JSONObject(); payload.put("payloadVersion", 1);
            payload.put("deviceId", "device-1"); payload.put("channelId", "channel-1");
            ServiceException failure = org.junit.jupiter.api.Assertions.assertThrows(ServiceException.class,
                () -> handler.execute(context("btask-busy", "bexec-busy"), payload));
            assertEquals(ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED.getCode(), failure.getCode());
            org.mockito.Mockito.verifyNoInteractions(leaseService, snapshotAdapter, storage);
        } finally {
            held.release();
            guards.remove("device-1:channel-1", held);
        }
    }

    @Test
    void execute_shouldClassifyStreamTimeoutAsRetryableAndReleaseNoResources() {
        ImageCollectionTaskHandler handler = wiredOnlineHandler("btask-timeout");
        when(leaseService.acquire("device-1", "channel-1"))
            .thenThrow(new ServiceException(ServiceExceptionEnum.IMAGE_STREAM_ESTABLISH_TIMEOUT));
        JSONObject payload = payload("device-1", "channel-1");
        ServiceException failure = assertThrows(ServiceException.class,
            () -> handler.execute(context("btask-timeout", "bexec-timeout"), payload));
        assertEquals(ServiceExceptionEnum.IMAGE_STREAM_ESTABLISH_TIMEOUT.getCode(), failure.getCode());
        assertTrue(handler.classify(failure, null).isRetryable());
        org.mockito.Mockito.verifyNoInteractions(snapshotAdapter, storage);
    }

    @Test
    void execute_shouldClassifySnapshotFailureAsRetryableAndCloseLease() throws Exception {
        ImageCollectionTaskHandler handler = wiredOnlineHandler("btask-snapshot-failure");
        io.github.lunasaw.voglander.service.live.MediaPlayService media = mock(io.github.lunasaw.voglander.service.live.MediaPlayService.class);
        when(leaseService.acquire("device-1", "channel-1"))
            .thenReturn(new CaptureStreamLease(media, "stream-failure", "node-1", "rtsp://camera/live"));
        when(snapshotAdapter.capture(any(MediaSnapshotCommand.class)))
            .thenThrow(new ServiceException(ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED));
        ServiceException failure = assertThrows(ServiceException.class,
            () -> handler.execute(context("btask-snapshot-failure", "bexec-snapshot-failure"), payload("device-1", "channel-1")));
        assertTrue(handler.classify(failure, null).isRetryable());
        verify(media).stopLive("stream-failure");
        org.mockito.Mockito.verifyNoInteractions(storage);
    }

    @Test
    void execute_shouldTreatCorruptImageAsPermanentAndDiscardStage() throws Exception {
        ImageCollectionTaskHandler handler = wiredOnlineHandler("btask-corrupt");
        io.github.lunasaw.voglander.service.live.MediaPlayService media = mock(io.github.lunasaw.voglander.service.live.MediaPlayService.class);
        when(leaseService.acquire("device-1", "channel-1"))
            .thenReturn(new CaptureStreamLease(media, "stream-corrupt", "node-1", "rtsp://camera/live"));
        when(snapshotAdapter.capture(any(MediaSnapshotCommand.class)))
            .thenReturn(new SnapshotContent(new ByteArrayInputStream(new byte[] {1}), 1, Instant.now(), () -> { }));
        when(storage.stage(any(ImageStageCommand.class), any())).thenReturn(new StagedImage("stage-corrupt", 1, "hash"));
        when(storage.openStaged("stage-corrupt")).thenReturn(new ByteArrayInputStream(new byte[] {1}));
        when(validation.inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class)))
            .thenThrow(new ServiceException(ServiceExceptionEnum.IMAGE_DECODE_FAILED));
        ServiceException failure = assertThrows(ServiceException.class,
            () -> handler.execute(context("btask-corrupt", "bexec-corrupt"), payload("device-1", "channel-1")));
        assertTrue(!handler.classify(failure, null).isRetryable());
        verify(storage).discardStaged("stage-corrupt");
        verify(media).stopLive("stream-corrupt");
    }

    @Test
    void execute_shouldClassifyPromoteStorageFailureAsRetryableAndCleanupStage() throws Exception {
        ImageCollectionTaskHandler handler = wiredOnlineHandler("btask-storage");
        io.github.lunasaw.voglander.service.live.MediaPlayService media = mock(io.github.lunasaw.voglander.service.live.MediaPlayService.class);
        when(leaseService.acquire("device-1", "channel-1"))
            .thenReturn(new CaptureStreamLease(media, "stream-storage", "node-1", "rtsp://camera/live"));
        when(snapshotAdapter.capture(any(MediaSnapshotCommand.class)))
            .thenReturn(new SnapshotContent(new ByteArrayInputStream(new byte[] {1}), 1, Instant.now(), () -> { }));
        when(storage.stage(any(ImageStageCommand.class), any())).thenReturn(new StagedImage("stage-storage", 1, "hash"));
        when(storage.openStaged("stage-storage")).thenReturn(new ByteArrayInputStream(new byte[] {1}));
        when(validation.inspect(any(), any(Long.class), any(), any(Long.class), any(Long.class)))
            .thenReturn(new VerifiedImage(ImageFormatEnum.JPEG, "image/jpeg", 1, 1, 1));
        when(storage.promote(any(ImagePromoteCommand.class))).thenThrow(new java.io.IOException("disk full"));
        java.io.IOException failure = assertThrows(java.io.IOException.class,
            () -> handler.execute(context("btask-storage", "bexec-storage"), payload("device-1", "channel-1")));
        assertTrue(handler.classify(failure, null).isRetryable());
        verify(storage).discardStaged("stage-storage");
        verify(media).stopLive("stream-storage");
    }

    private ImageCollectionTaskHandler wiredOnlineHandler(String taskId) {
        ImageCollectionTaskHandler handler = new ImageCollectionTaskHandler(configManager, leaseService, snapshotAdapter,
            storage, validation, new ImageProperties(), participant, deviceManager, channelManager);
        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId(taskId); config.setDeviceId("device-1"); config.setChannelId("channel-1");
        when(configManager.getByTaskId(taskId)).thenReturn(config);
        DeviceDTO device = new DeviceDTO(); device.setStatus(DeviceConstant.Status.ONLINE);
        DeviceChannelDTO channel = new DeviceChannelDTO(); channel.setStatus(DeviceConstant.Status.ONLINE);
        when(deviceManager.getDtoByDeviceId("device-1")).thenReturn(device);
        when(channelManager.getDtoByDeviceId("device-1", "channel-1")).thenReturn(channel);
        return handler;
    }

    private static JSONObject payload(String deviceId, String channelId) {
        JSONObject payload = new JSONObject(); payload.put("payloadVersion", 1);
        payload.put("deviceId", deviceId); payload.put("channelId", channelId); return payload;
    }

    private static Field cameraGuardsField() throws NoSuchFieldException {
        Field field = ImageCollectionTaskHandler.class.getDeclaredField("CAMERA_GUARDS");
        field.setAccessible(true);
        return field;
    }

    private static LongTaskContext context(String taskId, String executionId) {
        return context(taskId, executionId, new TaskCancellationToken() {
            @Override public boolean isCancellationRequested() { return false; }
            @Override public void throwIfCancellationRequested() { }
        });
    }

    private static LongTaskContext context(String taskId, String executionId, TaskCancellationToken token) {
        return new LongTaskContext() {
            @Override public String taskId() { return taskId; }
            @Override public String executionId() { return executionId; }
            @Override public int attempt() { return 1; }
            @Override public TaskCancellationToken cancellationToken() { return token; }
            @Override public void reportProgress(long current, long total, String message) { }
            @Override public boolean heartbeat() { return true; }
        };
    }
}
