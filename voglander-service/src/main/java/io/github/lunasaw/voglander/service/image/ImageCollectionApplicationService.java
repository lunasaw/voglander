package io.github.lunasaw.voglander.service.image;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.enums.image.ImageCollectionModeEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.common.enums.task.TaskModeEnum;
import io.github.lunasaw.voglander.client.domain.task.TaskCreateCommand;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionEnrichedDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskCommandDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lunasaw.voglander.manager.manager.BizTaskManager;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.manager.ImageCollectionConfigManager;

/** Domain facade that creates image configuration and the generic task atomically. */
@Service
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImageCollectionApplicationService {
    private final io.github.lunasaw.voglander.service.task.BizTaskCreateService taskCreateService;
    private final ImageCollectionConfigManager configManager;
    private final DeviceManager deviceManager;
    private final DeviceChannelManager channelManager;
    private final BizTaskManager taskManager;
    private final io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties properties;

    @Autowired
    public ImageCollectionApplicationService(io.github.lunasaw.voglander.service.task.BizTaskCreateService taskCreateService,
        ImageCollectionConfigManager configManager, DeviceManager deviceManager, DeviceChannelManager channelManager,
        io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties properties,
        BizTaskManager taskManager) {
        this.taskCreateService = taskCreateService;
        this.configManager = configManager;
        this.deviceManager = deviceManager;
        this.channelManager = channelManager;
        this.properties = properties;
        this.taskManager = taskManager;
    }

    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO create(ImageCollectionCreateCommand command) {
        Assert.notNull(command, "图像采集创建命令不能为空");
        validateBasic(command);
        DeviceDTO device = deviceManager.getDtoByDeviceId(command.deviceId());
        DeviceChannelDTO channel = channelManager.getDtoByDeviceId(command.deviceId(), command.channelId());
        if (device == null || channel == null || !command.deviceId().equals(channel.getDeviceId())) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_CAMERA_NOT_FOUND);
        }
        if (!isOnline(device.getStatus()) || !isOnline(channel.getStatus())) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_CAMERA_OFFLINE);
        }

        ImageCollectionModeEnum mode = parseMode(command.collectionMode());
        validateSchedule(command, mode);
        String retention = StringUtils.hasText(command.retentionPolicy()) ? command.retentionPolicy() : ImageConstant.RETENTION_PERMANENT;
        if (!ImageConstant.RETENTION_PERMANENT.equalsIgnoreCase(retention)) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID)
                .setDetailMessage("retentionPolicy must be PERMANENT");
        }
        String idempotency = StringUtils.hasText(command.idempotencyKey()) ? command.idempotencyKey() : internalKey();
        JSONObject payload = new JSONObject();
        payload.put("payloadVersion", ImageConstant.TASK_PAYLOAD_VERSION);
        payload.put("deviceId", command.deviceId());
        payload.put("channelId", command.channelId());
        payload.put("collectionMode", mode.name());

        String taskMode = mode == ImageCollectionModeEnum.ONCE ? TaskModeEnum.ONCE.name() : TaskModeEnum.FIXED_RATE.name();
        TaskCreateCommand taskCommand = new TaskCreateCommand(ImageConstant.TASK_TYPE_IMAGE_COLLECTION,
            command.taskName(), null, taskMode, mode == ImageCollectionModeEnum.ONCE ? null : command.scheduleStartTime(),
            mode == ImageCollectionModeEnum.ONCE ? null : command.scheduleEndTime(),
            mode == ImageCollectionModeEnum.ONCE ? null : command.intervalSeconds(), payload,
            ImageConstant.TASK_PAYLOAD_VERSION, "IMAGE_COLLECTION:" + command.deviceId() + ":" + command.channelId(),
            "CAMERA", command.deviceId(), command.ownerType(), command.ownerId(), command.organizationId(), idempotency, 3);
        BizTaskDTO task = taskCreateService.create(taskCommand);

        ImageCollectionConfigDTO config = new ImageCollectionConfigDTO();
        config.setTaskId(task.getTaskId());
        config.setDeviceId(command.deviceId());
        config.setChannelId(command.channelId());
        config.setDeviceNameSnapshot(StringUtils.hasText(device.getName()) ? device.getName() : command.deviceId());
        config.setChannelNameSnapshot(StringUtils.hasText(channel.getName()) ? channel.getName() : command.channelId());
        config.setRetentionPolicy(ImageConstant.RETENTION_PERMANENT);
        config.setCaptureOptions(payload);
        configManager.create(config);
        return task;
    }

    /** Returns generic task facts enriched with image configuration without copying task state. */
    public Page<ImageCollectionEnrichedDTO> getEnrichedPage(String taskName, String mode, String state,
        String deviceId, String channelId, String ownerType, String ownerId, int page, int size) {
        if (taskManager == null) throw new IllegalStateException("task manager is not wired");
        if (page <= 0 || size <= 0 || size > 1000) throw new ServiceException(ServiceExceptionEnum.PARAM_ERROR);
        BizTaskQueryDTO query = new BizTaskQueryDTO();
        query.setTaskType(ImageConstant.TASK_TYPE_IMAGE_COLLECTION);
        query.setTaskName(taskName);
        query.setState(state);
        query.setOwnerType(ownerType);
        query.setOwnerId(ownerId);
        if (StringUtils.hasText(mode)) {
            query.setTaskMode(modeToTaskMode(mode));
        }
        Page<BizTaskDTO> tasks = taskManager.getPage(query, BizTaskAccessScopeDTO.global(), page, size);
        Page<ImageCollectionEnrichedDTO> result = new Page<>(page, size, tasks.getTotal());
        result.setPages(tasks.getPages());
        java.util.List<ImageCollectionEnrichedDTO> records = new java.util.ArrayList<>();
        for (BizTaskDTO task : tasks.getRecords()) {
            ImageCollectionConfigDTO config = configManager.getByTaskId(task.getTaskId());
            if (config == null) continue;
            if (StringUtils.hasText(deviceId) && !deviceId.equals(config.getDeviceId())) continue;
            if (StringUtils.hasText(channelId) && !channelId.equals(config.getChannelId())) continue;
            ImageCollectionEnrichedDTO item = new ImageCollectionEnrichedDTO();
            item.setTask(task); item.setConfig(config); records.add(item);
        }
        result.setRecords(records);
        return result;
    }

    public ImageCollectionEnrichedDTO getEnrichedDetail(String taskId, String ownerType, String ownerId) {
        if (taskManager == null) throw new IllegalStateException("task manager is not wired");
        BizTaskQueryDTO query = new BizTaskQueryDTO(); query.setTaskId(taskId);
        query.setTaskType(ImageConstant.TASK_TYPE_IMAGE_COLLECTION); query.setOwnerType(ownerType); query.setOwnerId(ownerId);
        Page<BizTaskDTO> tasks = taskManager.getPage(query, BizTaskAccessScopeDTO.global(), 1, 1);
        if (tasks.getRecords().isEmpty()) return null;
        ImageCollectionConfigDTO config = configManager.getByTaskId(taskId);
        if (config == null) return null;
        ImageCollectionEnrichedDTO result = new ImageCollectionEnrichedDTO(); result.setTask(tasks.getRecords().get(0)); result.setConfig(config);
        return result;
    }

    /**
     * Reschedules only a paused image collection through the generic task manager.
     * Device/channel identity is intentionally read from the immutable image config;
     * this command can change timing only and never mutates the camera snapshot.
     */
    @Transactional(rollbackFor = Exception.class)
    public BizTaskDTO reschedule(BizTaskCommandDTO command) {
        if (taskManager == null) throw new IllegalStateException("task manager is not wired");
        Assert.notNull(command, "重排程命令不能为空");
        if (!StringUtils.hasText(command.getTaskId())) throw new ServiceException(ServiceExceptionEnum.PARAM_ERROR);
        BizTaskDTO task = taskManager.getByTaskId(command.getTaskId(), BizTaskAccessScopeDTO.global());
        if (task == null || !ImageConstant.TASK_TYPE_IMAGE_COLLECTION.equals(task.getTaskType())) {
            throw new ServiceException(ServiceExceptionEnum.TASK_NOT_FOUND);
        }
        if (!io.github.lunasaw.voglander.common.enums.task.TaskStateEnum.PAUSED.name().equals(task.getState())) {
            throw new ServiceException(ServiceExceptionEnum.TASK_STATE_CONFLICT);
        }
        if (!io.github.lunasaw.voglander.common.enums.task.TaskModeEnum.FIXED_RATE.name().equals(task.getTaskMode())) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID)
                .setDetailMessage("ONCE image collection cannot be rescheduled");
        }
        if (configManager.getByTaskId(task.getTaskId()) == null) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_CAMERA_NOT_FOUND);
        }
        ImageCollectionCreateCommand schedule = new ImageCollectionCreateCommand(
            task.getTaskName(), ImageCollectionModeEnum.SCHEDULED.name(), "snapshot-device", "snapshot-channel",
            command.getScheduleStartTime(), command.getScheduleEndTime(), command.getIntervalSeconds(),
            ImageConstant.RETENTION_PERMANENT, task.getOwnerType(), task.getOwnerId(), task.getOrganizationId(),
            task.getIdempotencyKey());
        // Reuse the exact image constraints without touching device/channel fields.
        validateSchedule(schedule, ImageCollectionModeEnum.SCHEDULED);
        BizTaskCommandDTO delegated = new BizTaskCommandDTO();
        delegated.setTaskId(command.getTaskId());
        delegated.setExpectedVersion(command.getExpectedVersion());
        delegated.setScheduleStartTime(command.getScheduleStartTime());
        delegated.setScheduleEndTime(command.getScheduleEndTime());
        delegated.setIntervalSeconds(command.getIntervalSeconds());
        delegated.setActorType(command.getActorType()); delegated.setActorId(command.getActorId());
        delegated.setReason(command.getReason()); delegated.setRequestedAt(command.getRequestedAt());
        return taskManager.reschedule(delegated);
    }

    private void validateBasic(ImageCollectionCreateCommand command) {
        if (!StringUtils.hasText(command.taskName()) || !StringUtils.hasText(command.deviceId())
            || !StringUtils.hasText(command.channelId()) || !StringUtils.hasText(command.ownerType())
            || !StringUtils.hasText(command.ownerId())) {
            throw new ServiceException(ServiceExceptionEnum.PARAM_ERROR);
        }
    }

    private void validateSchedule(ImageCollectionCreateCommand command, ImageCollectionModeEnum mode) {
        if (mode == ImageCollectionModeEnum.ONCE) {
            if (command.scheduleStartTime() != null || command.scheduleEndTime() != null || command.intervalSeconds() != null) {
                throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID);
            }
            return;
        }
        LocalDateTime start = command.scheduleStartTime();
        LocalDateTime end = command.scheduleEndTime();
        Long interval = command.intervalSeconds();
        if (start == null || end == null || interval == null || interval <= 0 || end.isBefore(start)
            || interval < properties.getCollection().getMinIntervalSeconds()) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID);
        }
        try {
            long count = Math.addExact(Duration.between(start, end).getSeconds() / interval, 1L);
            if (count > properties.getCollection().getMaxPlannedCount()) {
                throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_LIMIT_EXCEEDED);
            }
        } catch (ArithmeticException exception) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_LIMIT_EXCEEDED);
        }
    }

    private static ImageCollectionModeEnum parseMode(String value) {
        try {
            return ImageCollectionModeEnum.valueOf(value == null ? "" : value.toUpperCase());
        } catch (RuntimeException exception) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID);
        }
    }

    private static String modeToTaskMode(String value) {
        try {
            ImageCollectionModeEnum mode = ImageCollectionModeEnum.valueOf(value.toUpperCase());
            return mode == ImageCollectionModeEnum.ONCE ? TaskModeEnum.ONCE.name() : TaskModeEnum.FIXED_RATE.name();
        } catch (RuntimeException exception) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_COLLECTION_SCHEDULE_INVALID);
        }
    }

    private static boolean isOnline(Integer status) {
        return status != null && status.intValue() == DeviceConstant.Status.ONLINE;
    }

    private static String internalKey() {
        return "image-create-" + UUID.randomUUID().toString().replace("-", "");
    }
}
