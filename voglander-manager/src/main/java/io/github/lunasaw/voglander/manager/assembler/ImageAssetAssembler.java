package io.github.lunasaw.voglander.manager.assembler;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageCollectionConfigDTO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;

/** Explicit FastJSON2-backed mapping for image domain objects. */
public final class ImageAssetAssembler {
    private ImageAssetAssembler() {
    }

    public static ImageAssetDTO toDTO(ImageAssetDO source) {
        return source == null ? null : JSON.parseObject(JSON.toJSONString(source), ImageAssetDTO.class);
    }

    public static ImageAssetDO toDO(ImageAssetDTO source) {
        return source == null ? null : JSON.parseObject(JSON.toJSONString(source), ImageAssetDO.class);
    }

    public static ImageAssetSourceDTO toDTO(ImageAssetSourceDO source) {
        if (source == null) {
            return null;
        }
        ImageAssetSourceDTO dto = JSON.parseObject(JSON.toJSONString(source), ImageAssetSourceDTO.class);
        if (source.getSourceMetadata() != null) {
            dto.setSourceMetadata(JSON.parseObject(source.getSourceMetadata()));
        }
        return dto;
    }

    public static ImageAssetSourceDO toDO(ImageAssetSourceDTO source) {
        if (source == null) {
            return null;
        }
        ImageAssetSourceDO target = JSON.parseObject(JSON.toJSONString(source), ImageAssetSourceDO.class);
        target.setSourceMetadata(source.getSourceMetadata() == null ? null : JSON.toJSONString(source.getSourceMetadata()));
        return target;
    }

    public static ImageCollectionConfigDTO toDTO(ImageCollectionConfigDO source) {
        if (source == null) {
            return null;
        }
        ImageCollectionConfigDTO dto = JSON.parseObject(JSON.toJSONString(source), ImageCollectionConfigDTO.class);
        if (source.getCaptureOptions() != null) {
            dto.setCaptureOptions(JSON.parseObject(source.getCaptureOptions()));
        }
        return dto;
    }

    public static ImageCollectionConfigDO toDO(ImageCollectionConfigDTO source) {
        if (source == null) {
            return null;
        }
        ImageCollectionConfigDO target = JSON.parseObject(JSON.toJSONString(source), ImageCollectionConfigDO.class);
        target.setCaptureOptions(source.getCaptureOptions() == null ? null : JSON.toJSONString(source.getCaptureOptions()));
        return target;
    }
}
