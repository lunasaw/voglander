package io.github.lunasaw.voglander.service.image;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.service.idempotency.CanonicalJsonFingerprint;

/** Immutable canonical business identity for one image upload request. */
final class ImageUploadSnapshot {

    private final String ownerType;
    private final String ownerId;
    private final String organizationId;
    private final String checksum;
    private final String originalFilename;
    private final String assetName;

    private ImageUploadSnapshot(String ownerType, String ownerId, String organizationId, String checksum,
        String originalFilename, String assetName) {
        this.ownerType = normalized(ownerType);
        this.ownerId = normalized(ownerId);
        this.organizationId = normalized(organizationId);
        this.checksum = normalized(checksum);
        this.originalFilename = normalized(originalFilename);
        this.assetName = normalized(assetName);
    }

    static ImageUploadSnapshot from(ImageIngestCommand command, String checksum,
        ImageValidationService validation) {
        String originalFilename = validation.sanitizeOptionalFilename(command.originalFilename());
        String assetName = validation.sanitizeOptionalFilename(command.assetName());
        if (assetName == null) {
            assetName = originalFilename == null ? "unnamed" : originalFilename;
        }
        return new ImageUploadSnapshot(command.ownerType(), command.ownerId(), command.organizationId(), checksum,
            originalFilename, assetName);
    }

    static ImageUploadSnapshot from(ImageAssetDTO asset, ImageAssetSourceDTO source) {
        return new ImageUploadSnapshot(asset.getOwnerType(), asset.getOwnerId(), asset.getOrganizationId(),
            asset.getChecksum(), source == null ? null : source.getOriginalFilename(), asset.getAssetName());
    }

    String fingerprint() {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        fields.put("assetName", assetName);
        fields.put("checksum", checksum);
        fields.put("organizationId", organizationId);
        fields.put("originalFilename", originalFilename);
        fields.put("ownerId", ownerId);
        fields.put("ownerType", ownerType);
        return CanonicalJsonFingerprint.sha256(fields);
    }

    String getOriginalFilename() {
        return originalFilename;
    }

    String getAssetName() {
        return assetName;
    }

    private static String normalized(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.trim();
        return stripped.isEmpty() ? null : stripped;
    }
}
