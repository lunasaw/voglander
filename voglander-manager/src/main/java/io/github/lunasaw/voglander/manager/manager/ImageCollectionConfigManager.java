package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.manager.assembler.ImageAssetAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionEnrichedDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.manager.service.ImageCollectionConfigService;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionConfigMapper;

/** Manager for camera snapshots retained with a generic business task. */
@Component
public class ImageCollectionConfigManager {
    @Autowired
    private ImageCollectionConfigService configService;
    @Autowired
    private ImageCollectionConfigMapper configMapper;
    @Autowired
    private BizTaskManager taskManager;

    @Transactional(rollbackFor = Exception.class)
    public ImageCollectionConfigDTO create(ImageCollectionConfigDTO config) {
        Assert.notNull(config, "config不能为空");
        Assert.hasText(config.getTaskId(), "taskId不能为空");
        Assert.hasText(config.getDeviceId(), "deviceId不能为空");
        Assert.hasText(config.getChannelId(), "channelId不能为空");
        ImageCollectionConfigDO existing = configMapper.selectByTaskId(config.getTaskId());
        if (existing != null) return ImageAssetAssembler.toDTO(existing);
        LocalDateTime now = LocalDateTime.now();
        if (config.getCreateTime() == null) config.setCreateTime(now);
        if (config.getUpdateTime() == null) config.setUpdateTime(now);
        if (config.getRetentionPolicy() == null) config.setRetentionPolicy(ImageConstant.RETENTION_PERMANENT);
        if (config.getVersion() == null) config.setVersion(0);
        ImageCollectionConfigDO target = ImageAssetAssembler.toDO(config);
        if (configMapper.insertIfAbsent(target) == 0) target = configMapper.selectByTaskId(config.getTaskId());
        return ImageAssetAssembler.toDTO(target);
    }

    public ImageCollectionConfigDTO getByTaskId(String taskId) {
        return ImageAssetAssembler.toDTO(configMapper.selectByTaskId(taskId));
    }

    public Page<ImageCollectionConfigDTO> getPageByCamera(String deviceId, String channelId, long page, long size) {
        Assert.hasText(deviceId, "deviceId不能为空");
        Assert.hasText(channelId, "channelId不能为空");
        Page<ImageCollectionConfigDO> source = configMapper.selectPageByCamera(new Page<>(page, size), deviceId, channelId);
        Page<ImageCollectionConfigDTO> target = new Page<>(page, size, source.getTotal());
        target.setPages(source.getPages());
        target.setRecords(source.getRecords().stream().map(ImageAssetAssembler::toDTO).collect(Collectors.toList()));
        return target;
    }

    /** Combines generic task facts with this manager's immutable camera config. */
    public Page<ImageCollectionEnrichedDTO> getEnrichedPage(BizTaskQueryDTO query, BizTaskAccessScopeDTO scope,
        int page, int size) {
        BizTaskQueryDTO condition = query == null ? new BizTaskQueryDTO() : query;
        condition.setTaskType(ImageConstant.TASK_TYPE_IMAGE_COLLECTION);
        Page<BizTaskDTO> tasks = taskManager.getPage(condition, scope == null ? BizTaskAccessScopeDTO.global() : scope, page, size);
        Page<ImageCollectionEnrichedDTO> result = new Page<>(page, size, tasks.getTotal());
        result.setPages(tasks.getPages());
        java.util.List<ImageCollectionEnrichedDTO> records = new java.util.ArrayList<>();
        for (BizTaskDTO task : tasks.getRecords()) {
            ImageCollectionConfigDTO config = getByTaskId(task.getTaskId());
            if (config == null) continue;
            ImageCollectionEnrichedDTO item = new ImageCollectionEnrichedDTO(); item.setTask(task); item.setConfig(config); records.add(item);
        }
        result.setRecords(records);
        return result;
    }

    public ImageCollectionEnrichedDTO getEnrichedDetail(String taskId, BizTaskAccessScopeDTO scope) {
        BizTaskQueryDTO query = new BizTaskQueryDTO(); query.setTaskId(taskId); query.setTaskType(ImageConstant.TASK_TYPE_IMAGE_COLLECTION);
        Page<ImageCollectionEnrichedDTO> page = getEnrichedPage(query, scope, 1, 1);
        return page.getRecords().isEmpty() ? null : page.getRecords().get(0);
    }
}
