package io.github.lunasaw.voglander.service.image;

import java.io.IOException;

import org.springframework.stereotype.Service;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;

/** Applies stable state semantics before opening a private image object. */
@Service
public class ImageAssetReadService {

    private final ImageAssetManager assetManager;
    private final ImageStorageService storage;

    public ImageAssetReadService(ImageAssetManager assetManager, ImageStorageService storage) {
        this.assetManager = assetManager;
        this.storage = storage;
    }

    public ImageAssetDTO requireReadable(String assetId) {
        ImageAssetDTO asset = assetManager.getByAssetId(assetId);
        if (asset == null) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_ASSET_NOT_FOUND);
        }
        if ("DELETED".equals(asset.getStatus())) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_ASSET_GONE);
        }
        if (!"AVAILABLE".equals(asset.getStatus())) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_ASSET_STATE_CONFLICT);
        }
        return asset;
    }

    public ImageContent open(ImageAssetDTO asset) {
        try {
            return storage.open(asset.getStorageKey());
        } catch (IOException exception) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_STORAGE_READ_FAILED)
                .setDetailMessage("provider-read");
        }
    }
}
