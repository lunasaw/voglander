package io.github.lunasaw.voglander.service.image;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.image.ImagePromoteCommand;
import io.github.lunasaw.voglander.client.domain.image.ImageStageCommand;
import io.github.lunasaw.voglander.client.domain.image.MediaSnapshotCommand;
import io.github.lunasaw.voglander.client.domain.image.SnapshotContent;
import io.github.lunasaw.voglander.client.domain.image.StoredImage;
import io.github.lunasaw.voglander.client.domain.task.RetryDecision;
import io.github.lunasaw.voglander.client.domain.task.TaskAttemptContext;
import io.github.lunasaw.voglander.client.domain.task.TaskCapabilities;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateContext;
import io.github.lunasaw.voglander.client.domain.task.TaskExecutionResult;
import io.github.lunasaw.voglander.client.domain.task.TaskResultReference;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.client.service.image.MediaSnapshotAdapter;
import io.github.lunasaw.voglander.client.service.task.LongTaskContext;
import io.github.lunasaw.voglander.client.service.task.LongTaskHandler;
import io.github.lunasaw.voglander.common.constant.task.TaskConstant;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.enums.image.ImageAssetSourceTypeEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.manager.manager.ImageCollectionConfigManager;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;

/** Durable IMAGE_COLLECTION Handler; task/execution state remains owned by the generic engine. */
@Component
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImageCollectionTaskHandler implements LongTaskHandler {
    private static final DateTimeFormatter PARTITION = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final ImageCollectionConfigManager configManager;
    private final CaptureStreamLeaseService leaseService;
    private final MediaSnapshotAdapter snapshotAdapter;
    private final ImageStorageService storage;
    private final ImageValidationService validation;
    private final ImageProperties properties;
    private final ImageCollectionCompletionParticipant participant;
    private final DeviceManager deviceManager;
    private final DeviceChannelManager channelManager;
    private final ImageOrphanRecorder orphanRecorder;
    private final ImageDomainMetrics metrics;
    /** JVM-local competition guard; the durable execution claim remains authoritative across nodes. */
    private static final ConcurrentHashMap<String, Semaphore> CAMERA_GUARDS = new ConcurrentHashMap<>();

    /** Lightweight constructor retained for contract tests that only exercise SPI facts. */
    public ImageCollectionTaskHandler() {
        this.configManager = null;
        this.leaseService = null;
        this.snapshotAdapter = null;
        this.storage = null;
        this.validation = null;
        this.properties = null;
        this.participant = null;
        this.deviceManager = null;
        this.channelManager = null;
        this.orphanRecorder = null;
        this.metrics = null;
    }

    public ImageCollectionTaskHandler(ImageCollectionConfigManager configManager,
        CaptureStreamLeaseService leaseService, MediaSnapshotAdapter snapshotAdapter, ImageStorageService storage,
        ImageValidationService validation, ImageProperties properties,
        ImageCollectionCompletionParticipant participant, DeviceManager deviceManager, DeviceChannelManager channelManager) {
        this(configManager, leaseService, snapshotAdapter, storage, validation, properties, participant,
            deviceManager, channelManager, null, null);
    }

    public ImageCollectionTaskHandler(ImageCollectionConfigManager configManager,
        CaptureStreamLeaseService leaseService, MediaSnapshotAdapter snapshotAdapter, ImageStorageService storage,
        ImageValidationService validation, ImageProperties properties,
        ImageCollectionCompletionParticipant participant, DeviceManager deviceManager, DeviceChannelManager channelManager,
        ImageOrphanRecorder orphanRecorder) {
        this(configManager, leaseService, snapshotAdapter, storage, validation, properties, participant,
            deviceManager, channelManager, orphanRecorder, null);
    }

    @Autowired
    public ImageCollectionTaskHandler(ImageCollectionConfigManager configManager,
        CaptureStreamLeaseService leaseService, MediaSnapshotAdapter snapshotAdapter, ImageStorageService storage,
        ImageValidationService validation, ImageProperties properties,
        ImageCollectionCompletionParticipant participant, DeviceManager deviceManager, DeviceChannelManager channelManager,
        ImageOrphanRecorder orphanRecorder, ImageDomainMetrics metrics) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.leaseService = Objects.requireNonNull(leaseService, "leaseService");
        this.snapshotAdapter = Objects.requireNonNull(snapshotAdapter, "snapshotAdapter");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.participant = Objects.requireNonNull(participant, "participant");
        this.deviceManager = Objects.requireNonNull(deviceManager, "deviceManager");
        this.channelManager = Objects.requireNonNull(channelManager, "channelManager");
        this.orphanRecorder = orphanRecorder;
        this.metrics = metrics;
    }

    @Override public String taskType() { return ImageConstant.TASK_TYPE_IMAGE_COLLECTION; }
    @Override public int payloadVersion() { return ImageConstant.TASK_PAYLOAD_VERSION; }
    @Override public TaskCapabilities capabilities() { return new TaskCapabilities(true, true, true, true, true); }

    @Override
    public void validate(TaskCreateContext context, JSONObject payload) {
        if (context == null || !StringUtils.hasText(context.ownerId())) throw new ServiceException(ServiceExceptionEnum.TASK_PERMISSION_DENIED);
        if (payload == null || !StringUtils.hasText(payload.getString("deviceId"))
            || !StringUtils.hasText(payload.getString("channelId"))) {
            throw new ServiceException(ServiceExceptionEnum.TASK_PAYLOAD_INVALID);
        }
        if (payload.getIntValue("payloadVersion") != ImageConstant.TASK_PAYLOAD_VERSION) {
            throw new ServiceException(ServiceExceptionEnum.TASK_PAYLOAD_INVALID);
        }
        if (payload.containsKey("secret") || payload.containsKey("url") || payload.containsKey("storageKey") || payload.containsKey("path")) {
            throw new ServiceException(ServiceExceptionEnum.TASK_PAYLOAD_INVALID);
        }
    }

    @Override
    public TaskExecutionResult execute(LongTaskContext context, JSONObject payload) throws Exception {
        if (configManager == null) throw new IllegalStateException("IMAGE_COLLECTION Handler is not wired");
        validate(new TaskCreateContext("SYSTEM", "worker", null, "CAMERA", payload == null ? null : payload.getString("channelId")), payload);
        context.cancellationToken().throwIfCancellationRequested();
        String taskId = context.taskId();
        if (!StringUtils.hasText(taskId)) throw new ServiceException(ServiceExceptionEnum.TASK_PAYLOAD_INVALID);
        ImageCollectionConfigDTO config = configManager.getByTaskId(taskId);
        if (config == null) throw new ServiceException(ServiceExceptionEnum.IMAGE_CAMERA_NOT_FOUND);
        if (!config.getDeviceId().equals(payload.getString("deviceId"))
            || !config.getChannelId().equals(payload.getString("channelId"))) {
            throw new ServiceException(ServiceExceptionEnum.TASK_PAYLOAD_INVALID);
        }
        DeviceDTO device = deviceManager.getDtoByDeviceId(config.getDeviceId());
        DeviceChannelDTO channel = channelManager.getDtoByDeviceId(config.getDeviceId(), config.getChannelId());
        if (device == null || channel == null) throw new ServiceException(ServiceExceptionEnum.IMAGE_CAMERA_NOT_FOUND);
        if (device.getStatus() == null || channel.getStatus() == null
            || device.getStatus().intValue() != io.github.lunasaw.voglander.common.constant.device.DeviceConstant.Status.ONLINE
            || channel.getStatus().intValue() != io.github.lunasaw.voglander.common.constant.device.DeviceConstant.Status.ONLINE) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_CAMERA_OFFLINE);
        }
        String guardKey = config.getDeviceId() + ":" + config.getChannelId();
        Semaphore guard = CAMERA_GUARDS.computeIfAbsent(guardKey, ignored -> new Semaphore(1));
        if (!guard.tryAcquire()) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED)
                .setDetailMessage("camera capture is already in progress");
        }
        CaptureStreamLease lease = null;
        String assetId = "img_" + UUID.randomUUID().toString().replace("-", "");
        io.github.lunasaw.voglander.client.domain.image.StagedImage staged = null;
        String finalKey = null;
        ImageLogContext logContext = ImageLogContext.open(taskId, context.executionId(), assetId,
            config.getDeviceId(), config.getChannelId(), null);
        try {
            lease = leaseService.acquire(config.getDeviceId(), config.getChannelId());
            context.cancellationToken().throwIfCancellationRequested();
            long snapshotStarted = System.nanoTime();
            try (SnapshotContent snapshot = snapshotAdapter.capture(new MediaSnapshotCommand(lease.getNodeServerId(), lease.getSnapshotUrl(),
                properties.getSnapshot().getTimeoutSeconds()))) {
                staged = storage.stage(new ImageStageCommand(assetId, properties.getStorage().getWorkerNode(),
                    properties.getStorage().getMaxUploadBytes()), snapshot.inputStream());
            } finally {
                if (metrics != null) metrics.snapshot(Duration.ofNanos(System.nanoTime() - snapshotStarted), lease.getNodeServerId());
            }
            // Staging can block on a bounded stream; observe cooperative cancellation before
            // opening the staged object for validation.
            context.cancellationToken().throwIfCancellationRequested();
            VerifiedImage verified;
            try (java.io.InputStream input = storage.openStaged(staged.stagingKey())) {
                verified = validation.inspect(input, staged.fileSize(), null, properties.getStorage().getMaxUploadBytes(),
                    properties.getCollection().getMaxPixels());
            }
            context.cancellationToken().throwIfCancellationRequested();
            LocalDateTime capturedAt = LocalDateTime.now();
            finalKey = "images/" + PARTITION.format(capturedAt) + "/" + assetId + "." + verified.format().getExtension();
            long storageStarted = System.nanoTime();
            StoredImage stored;
            try {
                stored = storage.promote(new ImagePromoteCommand(staged.stagingKey(), finalKey));
            } finally {
                if (metrics != null) metrics.storage(Duration.ofNanos(System.nanoTime() - storageStarted), "PROMOTE", "LOCAL");
            }
            ImageAssetDTO asset = new ImageAssetDTO();
            asset.setAssetId(assetId); asset.setAssetName(assetId); asset.setStorageProvider("LOCAL");
            asset.setStorageKey(stored.storageKey()); asset.setContentType(verified.contentType()); asset.setImageFormat(verified.format().name());
            asset.setFileSize(verified.fileSize()); asset.setWidth(verified.width()); asset.setHeight(verified.height());
            asset.setChecksumAlgorithm("SHA256"); asset.setChecksum(staged.checksum()); asset.setCapturedAt(capturedAt);
            asset.setOwnerType("SYSTEM"); asset.setOwnerId("IMAGE_COLLECTION"); asset.setRetentionPolicy("PERMANENT");
            ImageAssetSourceDTO source = new ImageAssetSourceDTO(); source.setAssetId(assetId);
            source.setSourceType(ImageAssetSourceTypeEnum.CAMERA_CAPTURE.name()); source.setSourceSystem("VOGLANDER_CAPTURE");
            source.setSourceEntityType("CAMERA"); source.setSourceEntityId(config.getDeviceId() + ":" + config.getChannelId());
            JSONObject metadata = new JSONObject(); metadata.put("deviceId", config.getDeviceId()); metadata.put("channelId", config.getChannelId());
            // Persist stable diagnostics only; provider URLs, credentials and storage paths are never source metadata.
            metadata.put("protocol", "RTSP");
            metadata.put("nodeServerId", lease.getNodeServerId());
            metadata.put("streamId", lease.getStreamId());
            source.setSourceMetadata(metadata);
            JSONObject completion = new JSONObject(); completion.put("asset", JSONObject.parseObject(com.alibaba.fastjson2.JSON.toJSONString(asset)));
            completion.put("source", JSONObject.parseObject(com.alibaba.fastjson2.JSON.toJSONString(source)));
            JSONObject summary = new JSONObject(); summary.put("assetId", assetId); summary.put("width", verified.width()); summary.put("height", verified.height());
            final String promotedKey = finalKey;
            TaskExecutionResult result = new TaskExecutionResult(new TaskResultReference("IMAGE_ASSET", assetId), summary, completion,
                () -> {
                    try {
                        if (!storage.delete(promotedKey) && orphanRecorder != null) {
                            orphanRecorder.record("COMPENSATION_DELETE_FALSE", promotedKey);
                        }
                    } catch (Exception exception) {
                        if (orphanRecorder != null) orphanRecorder.record("COMPENSATION_DELETE_FAILED", promotedKey);
                        throw new IllegalStateException("image provider compensation failed", exception);
                    }
                });
            if (metrics != null) metrics.handler("SUCCESS", null);
            return result;
        } catch (Exception failure) {
            if (metrics != null) {
                String code = failure instanceof ServiceException
                    ? String.valueOf(((ServiceException) failure).getCode()) : "IMAGE_HANDLER_FAILED";
                metrics.handler("FAILED", code);
            }
            throw failure;
        } finally {
            try { if (staged != null) storage.discardStaged(staged.stagingKey()); } catch (Exception ignored) { }
            try { if (lease != null) lease.close(); } finally {
                guard.release();
                if (guard.availablePermits() == 1) CAMERA_GUARDS.remove(guardKey, guard);
            }
            logContext.close();
        }
    }

    @Override
    public RetryDecision classify(Throwable throwable, TaskAttemptContext context) {
        String code = "IMAGE_HANDLER_FAILED";
        boolean retryable = throwable instanceof IOException || throwable instanceof java.util.concurrent.TimeoutException;
        if (throwable instanceof ServiceException) {
            int serviceCode = ((ServiceException) throwable).getCode();
            retryable = serviceCode == ServiceExceptionEnum.IMAGE_STREAM_ESTABLISH_TIMEOUT.getCode()
                || serviceCode == ServiceExceptionEnum.IMAGE_SNAPSHOT_FAILED.getCode()
                || serviceCode == ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED.getCode();
            code = serviceCode == ServiceExceptionEnum.IMAGE_CAMERA_OFFLINE.getCode() ? "IMAGE_CAMERA_OFFLINE" : code;
        }
        return retryable ? RetryDecision.retryable(code, "image collection failed") : RetryDecision.permanent(code, "image collection failed");
    }

    @Override public io.github.lunasaw.voglander.client.service.task.TaskCompletionParticipant completionParticipant() {
        return participant == null ? io.github.lunasaw.voglander.client.service.task.TaskCompletionParticipant.noop() : participant;
    }
}
