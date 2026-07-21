package io.github.lunasaw.voglander.manager.manager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.enums.image.ImageAssetStatusEnum;
import io.github.lunasaw.voglander.manager.assembler.ImageAssetAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetStatisticsDTO;
import io.github.lunasaw.voglander.manager.service.ImageAssetService;
import io.github.lunasaw.voglander.manager.service.ImageAssetSourceService;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;
import io.github.lunasaw.voglander.repository.mapper.ImageAssetMapper;
import io.github.lunasaw.voglander.repository.mapper.ImageAssetSourceMapper;

/** Transaction boundary for image assets and their immutable primary source. */
@Component
public class ImageAssetManager {

    @Autowired(required = false)
    private BusinessTaskSseEventPublisher sseEventPublisher;

    @Autowired
    private ImageAssetService assetService;
    @Autowired
    private ImageAssetSourceService sourceService;
    @Autowired
    private ImageAssetMapper assetMapper;
    @Autowired
    private ImageAssetSourceMapper sourceMapper;

    @Transactional(rollbackFor = Exception.class)
    public ImageAssetDTO createWithSource(ImageAssetDTO asset, ImageAssetSourceDTO source) {
        Assert.notNull(asset, "asset不能为空");
        Assert.notNull(source, "source不能为空");
        Assert.hasText(asset.getAssetId(), "assetId不能为空");
        Assert.hasText(asset.getAssetName(), "assetName不能为空");
        Assert.hasText(asset.getOwnerType(), "ownerType不能为空");
        Assert.hasText(asset.getOwnerId(), "ownerId不能为空");
        Assert.hasText(asset.getStorageKey(), "storageKey不能为空");
        Assert.hasText(source.getAssetId(), "source.assetId不能为空");
        Assert.isTrue(asset.getAssetId().equals(source.getAssetId()), "asset/source identity不一致");
        ImageAssetDTO existing = getByAssetId(asset.getAssetId());
        if (existing != null) {
            return existing;
        }
        if (StringUtils.hasText(asset.getIdempotencyKey())) {
            existing = findByIdempotency(asset.getOwnerType(), asset.getOwnerId(), asset.getIdempotencyKey());
            if (existing != null) {
                return existing;
            }
        }
        LocalDateTime now = LocalDateTime.now();
        if (asset.getCreateTime() == null) asset.setCreateTime(now);
        if (asset.getUpdateTime() == null) asset.setUpdateTime(now);
        if (asset.getCapturedAt() == null) asset.setCapturedAt(now);
        if (asset.getIngestedAt() == null) asset.setIngestedAt(now);
        if (!StringUtils.hasText(asset.getStatus())) asset.setStatus(ImageAssetStatusEnum.AVAILABLE.name());
        if (!StringUtils.hasText(asset.getStorageProvider())) asset.setStorageProvider("LOCAL");
        if (!StringUtils.hasText(asset.getRetentionPolicy())) asset.setRetentionPolicy(ImageConstant.RETENTION_PERMANENT);
        if (!StringUtils.hasText(asset.getChecksumAlgorithm())) asset.setChecksumAlgorithm(ImageConstant.CHECKSUM_ALGORITHM);
        if (asset.getVersion() == null) asset.setVersion(0);
        ImageAssetDO assetDO = ImageAssetAssembler.toDO(asset);
        if (assetMapper.insertIfAbsent(assetDO) == 0) {
            return getByAssetId(asset.getAssetId());
        }
        source.setCreateTime(source.getCreateTime() == null ? now : source.getCreateTime());
        ImageAssetSourceDO sourceDO = ImageAssetAssembler.toDO(source);
        if (sourceMapper.insertIfAbsent(sourceDO) == 0) {
            ImageAssetSourceDO accepted = sourceMapper.selectByAssetId(source.getAssetId());
            if (accepted == null) throw new IllegalStateException("image source insert was ignored without an existing source");
        }
        ImageAssetDTO accepted = ImageAssetAssembler.toDTO(assetDO);
        publishAssetEvent(accepted, "ASSET_CREATED");
        return accepted;
    }

    public ImageAssetDTO findByIdempotency(String ownerType, String ownerId, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) return null;
        return ImageAssetAssembler.toDTO(assetMapper.selectByIdempotency(ownerType, ownerId, idempotencyKey));
    }

    public ImageAssetDTO getByAssetId(String assetId) {
        return ImageAssetAssembler.toDTO(assetMapper.selectByAssetId(assetId));
    }

    public ImageAssetDTO getByAssetId(String assetId, String ownerType, String ownerId) {
        ImageAssetDTO asset = getByAssetId(assetId);
        if (asset == null) return null;
        // A null owner pair represents the module-level image scope. The Web layer has already enforced the
        // image permission; non-null pairs retain the narrower owner policy for service-to-service callers.
        if (!StringUtils.hasText(ownerType) && !StringUtils.hasText(ownerId)) return asset;
        if (!Objects.equals(asset.getOwnerType(), ownerType) || !Objects.equals(asset.getOwnerId(), ownerId)) {
            return null;
        }
        return asset;
    }

    public ImageAssetSourceDTO getSourceByAssetId(String assetId) {
        return ImageAssetAssembler.toDTO(sourceMapper.selectByAssetId(assetId));
    }

    public ImageAssetSourceDTO getSourceByExecutionId(String executionId) {
        return ImageAssetAssembler.toDTO(sourceMapper.selectByExecutionId(executionId));
    }

    public ImageAssetSourceDTO getSourceByTaskId(String taskId) {
        return ImageAssetAssembler.toDTO(sourceMapper.selectByTaskId(taskId));
    }

    public Page<ImageAssetDTO> getPage(ImageAssetQueryDTO query, long page, long size) {
        Assert.isTrue(page > 0, "page必须大于0");
        Assert.isTrue(size > 0 && size <= 1000, "size必须在1-1000之间");
        Assert.notNull(query, "query不能为空");
        Page<ImageAssetDO> source = assetMapper.selectPageByCondition(new Page<>(page, size), query.getOwnerType(),
            query.getOwnerId(), query.getAssetId(), query.getAssetName(), query.getStatus(), query.getSourceType(),
            query.getSourceTaskId(), query.getSourceExecutionId(), query.getDeviceId(), query.getChannelId(),
            query.getCapturedStart(), query.getCapturedEnd());
        Page<ImageAssetDTO> target = new Page<>(page, size, source.getTotal());
        target.setPages(source.getPages());
        target.setRecords(source.getRecords().stream().map(ImageAssetAssembler::toDTO).collect(Collectors.toList()));
        return target;
    }

    public ImageAssetStatisticsDTO statistics(String ownerType, String ownerId) {
        ImageAssetStatisticsDTO stats = new ImageAssetStatisticsDTO();
        stats.setTotal(assetMapper.countVisible(ownerType, ownerId, null, null));
        stats.setAvailable(assetMapper.countVisible(ownerType, ownerId, ImageAssetStatusEnum.AVAILABLE.name(), null));
        stats.setDeleteFailed(assetMapper.countVisible(ownerType, ownerId, ImageAssetStatusEnum.DELETE_FAILED.name(), null));
        stats.setToday(assetMapper.countVisible(ownerType, ownerId, null, LocalDate.now().atStartOfDay()));
        return stats;
    }

    public boolean markDeleting(String assetId, int version, LocalDateTime now) {
        boolean changed = assetMapper.markDeleting(assetId, version, now) == 1;
        if (changed) publishAssetEvent(getByAssetId(assetId), "ASSET_DELETING");
        return changed;
    }

    public boolean markDeleted(String assetId, int version, LocalDateTime now) {
        boolean changed = assetMapper.markDeleted(assetId, version, now, now) == 1;
        if (changed) publishAssetEvent(getByAssetId(assetId), "ASSET_DELETED");
        return changed;
    }

    public boolean markDeleteFailed(String assetId, int version, String failureCode, String failureMessage,
        LocalDateTime now) {
        boolean changed = assetMapper.markDeleteFailed(assetId, version, sanitize(failureCode, 64), sanitize(failureMessage, 512), now) == 1;
        if (changed) publishAssetEvent(getByAssetId(assetId), "ASSET_DELETE_FAILED");
        return changed;
    }

    private void publishAssetEvent(ImageAssetDTO asset, String eventType) {
        if (asset == null || sseEventPublisher == null) return;
        String topic = "ASSET_DELETED".equals(eventType) || "ASSET_DELETE_FAILED".equals(eventType)
            || "ASSET_DELETING".equals(eventType) ? ImageConstant.SSE_ASSET_DELETED : ImageConstant.SSE_ASSET_CREATED;
        sseEventPublisher.publish(new io.github.lunasaw.voglander.common.event.BusinessTaskSseEvent(
            topic, asset.getAssetId(), null, "IMAGE_ASSET", asset.getStatus(), null,
            eventType, null, null, null, null, null, null, System.currentTimeMillis()));
    }

    /** Returns true for an already deleted asset and performs a conditional transition otherwise. */
    @Transactional(rollbackFor = Exception.class)
    public boolean requestDelete(String assetId, String ownerType, String ownerId, LocalDateTime now) {
        ImageAssetDTO asset = getByAssetId(assetId, ownerType, ownerId);
        if (asset == null) return false;
        if (ImageAssetStatusEnum.DELETED.name().equals(asset.getStatus())) return true;
        if (ImageAssetStatusEnum.DELETING.name().equals(asset.getStatus())) return true;
        return markDeleting(assetId, asset.getVersion() == null ? 0 : asset.getVersion(), now);
    }

    private static String sanitize(String value, int max) {
        if (value == null) return null;
        String safe = value.replaceAll("[\\r\\n\\t]", " ").replaceAll("\\s+", " ").trim();
        return safe.length() <= max ? safe : safe.substring(0, max);
    }
}
