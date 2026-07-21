package io.github.lunasaw.voglander.service.image;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.enums.image.ImageAssetStatusEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;

/** Provider-aware, CAS-protected image deletion lifecycle. */
@Service
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImageAssetLifecycleService {
    private final ImageAssetManager assetManager;
    private final ImageStorageService storage;

    public ImageAssetLifecycleService(ImageAssetManager assetManager, ImageStorageService storage) {
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String assetId, String ownerType, String ownerId) {
        return deleteInternal(assetId, ownerType, ownerId, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean retryDelete(String assetId, String ownerType, String ownerId) {
        return deleteInternal(assetId, ownerType, ownerId, true);
    }

    private boolean deleteInternal(String assetId, String ownerType, String ownerId, boolean retry) {
        ImageAssetDTO asset = assetManager.getByAssetId(assetId, ownerType, ownerId);
        if (asset == null) throw new ServiceException(ServiceExceptionEnum.IMAGE_ASSET_NOT_FOUND);
        String status = asset.getStatus();
        if (ImageAssetStatusEnum.DELETED.name().equals(status)) return true;
        if (ImageAssetStatusEnum.DELETING.name().equals(status) && !retry) return true;
        if (retry && !ImageAssetStatusEnum.DELETE_FAILED.name().equals(status)) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT);
        }
        int version = asset.getVersion() == null ? 0 : asset.getVersion();
        LocalDateTime now = LocalDateTime.now();
        if (!assetManager.markDeleting(assetId, version, now)) {
            ImageAssetDTO current = assetManager.getByAssetId(assetId, ownerType, ownerId);
            if (current != null && ImageAssetStatusEnum.DELETED.name().equals(current.getStatus())) return true;
            throw new ServiceException(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT);
        }
        ImageAssetDTO deleting = assetManager.getByAssetId(assetId);
        if (deleting == null) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT);
        }
        try {
            boolean removed = storage.delete(deleting.getStorageKey());
            if (!removed) {
                // Providers may report a missing object as a non-success result.  A second
                // existence probe makes deletion idempotent without treating an existing object
                // as deleted when the provider really failed.
                if (storage.exists(deleting.getStorageKey())) {
                    throw new IOException("provider deletion failed");
                }
                removed = true;
            }
            return assetManager.markDeleted(assetId, deleting.getVersion() == null ? version + 1 : deleting.getVersion(), LocalDateTime.now());
        } catch (Exception exception) {
            int deletingVersion = deleting.getVersion() == null ? version + 1 : deleting.getVersion();
            assetManager.markDeleteFailed(assetId, deletingVersion, "STORAGE_DELETE_FAILED", "provider deletion failed", LocalDateTime.now());
            throw new ServiceException(ServiceExceptionEnum.IMAGE_STORAGE_DELETE_FAILED)
                .setDetailMessage(exception.getClass().getSimpleName());
        }
    }
}
