package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.manager.assembler.ImageAssetAssembler;
import io.github.lunasaw.voglander.manager.assembler.BizTaskAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionEnrichedDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskAccessScopeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskQueryDTO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionTaskDO;
import io.github.lunasaw.voglander.repository.domain.image.ImageCollectionTaskQueryCondition;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionConfigMapper;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionTaskReadMapper;

/** Manager for camera snapshots retained with a generic business task. */
@Component
public class ImageCollectionConfigManager {
    @Autowired
    private ImageCollectionConfigMapper configMapper;
    @Autowired
    private ImageCollectionTaskReadMapper taskReadMapper;
    @Autowired
    private BizTaskAssembler taskAssembler;

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
        return getEnrichedPage(query, null, null, scope, page, size);
    }

    public Page<ImageCollectionEnrichedDTO> getEnrichedPage(BizTaskQueryDTO query, String deviceId,
        String channelId, BizTaskAccessScopeDTO scope, int page, int size) {
        Assert.isTrue(page > 0, "page必须大于0");
        Assert.isTrue(size > 0 && size <= 1000, "size必须在1-1000之间");
        ImageCollectionTaskQueryCondition condition = condition(query, deviceId, channelId, scope);
        Page<ImageCollectionTaskDO> source = taskReadMapper.selectPageByCondition(
            new Page<ImageCollectionTaskDO>(page, size), condition);
        Page<ImageCollectionEnrichedDTO> result = new Page<ImageCollectionEnrichedDTO>(page, size,
            source.getTotal());
        result.setPages(source.getPages());
        result.setRecords(source.getRecords().stream().map(this::toEnrichedDTO).collect(Collectors.toList()));
        return result;
    }

    public ImageCollectionEnrichedDTO getEnrichedDetail(String taskId, BizTaskAccessScopeDTO scope) {
        Assert.hasText(taskId, "taskId不能为空");
        BizTaskQueryDTO query = new BizTaskQueryDTO();
        query.setTaskId(taskId);
        return toEnrichedDTO(taskReadMapper.selectDetailByCondition(condition(query, null, null, scope)));
    }

    private ImageCollectionTaskQueryCondition condition(BizTaskQueryDTO query, String deviceId,
        String channelId, BizTaskAccessScopeDTO scope) {
        BizTaskQueryDTO filter = query == null ? new BizTaskQueryDTO() : query;
        BizTaskAccessScopeDTO trustedScope = scope == null ? BizTaskAccessScopeDTO.global() : scope;
        Assert.isTrue(trustedScope.getAllowedTaskTypes() == null || !trustedScope.getAllowedTaskTypes().isEmpty(),
            "任务类型访问范围不能为空集合");
        boolean hasOwner = StringUtils.hasText(trustedScope.getOwnerType())
            && StringUtils.hasText(trustedScope.getOwnerId());
        boolean hasOrganization = StringUtils.hasText(trustedScope.getOrganizationId());
        Assert.isTrue(trustedScope.isGlobalScope() || hasOwner || hasOrganization,
            "非全局任务范围必须包含 owner 或 organization");
        ImageCollectionTaskQueryCondition condition = new ImageCollectionTaskQueryCondition();
        condition.setTaskId(filter.getTaskId());
        condition.setTaskNameLike(StringUtils.hasText(filter.getTaskName()) ? "%" + filter.getTaskName() + "%" : null);
        condition.setTaskMode(filter.getTaskMode());
        condition.setState(filter.getState());
        condition.setDeviceId(deviceId);
        condition.setChannelId(channelId);
        condition.setFilterOwnerType(filter.getOwnerType());
        condition.setFilterOwnerId(filter.getOwnerId());
        condition.setFilterOrganizationId(filter.getOrganizationId());
        condition.setGlobal(trustedScope.isGlobalScope());
        condition.setOwnerType(trustedScope.getOwnerType());
        condition.setOwnerId(trustedScope.getOwnerId());
        condition.setOrganizationId(trustedScope.getOrganizationId());
        condition.setAllowedTaskTypes(trustedScope.getAllowedTaskTypes());
        return condition;
    }

    private ImageCollectionEnrichedDTO toEnrichedDTO(ImageCollectionTaskDO source) {
        if (source == null) {
            return null;
        }
        ImageCollectionEnrichedDTO target = new ImageCollectionEnrichedDTO();
        target.setTask(taskAssembler.doToSafeDto(source.getTask()));
        target.setConfig(ImageAssetAssembler.toDTO(source.getConfig()));
        return target;
    }
}
