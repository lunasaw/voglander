package io.github.lunasaw.voglander.service.image;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.client.domain.image.ImagePromoteCommand;
import io.github.lunasaw.voglander.client.domain.image.ImageStageCommand;
import io.github.lunasaw.voglander.client.domain.image.StagedImage;
import io.github.lunasaw.voglander.client.domain.image.StoredImage;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.enums.image.ImageAssetSourceTypeEnum;
import io.github.lunasaw.voglander.common.enums.image.ImageStorageProviderEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetCreateResultDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import io.github.lunasaw.voglander.service.idempotency.IdempotencyKeyValidator;
import io.github.lunasaw.voglander.service.idempotency.IdempotencyMetrics;

/** Stage, verify, promote and register one upload with compensating cleanup. */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImageIngestService {
    private final ImageStorageService storage;
    private final ImageValidationService validation;
    private final ImageAssetManager assetManager;
    private final ImageProperties properties;
    private final ImageOrphanRecorder orphanRecorder;
    private final ImageDomainMetrics metrics;
    private final IdempotencyMetrics idempotencyMetrics;

    public ImageIngestService(ImageStorageService storage, ImageValidationService validation,
        ImageAssetManager assetManager, ImageProperties properties) {
        this(storage, validation, assetManager, properties, null, null);
    }

    public ImageIngestService(ImageStorageService storage, ImageValidationService validation,
        ImageAssetManager assetManager, ImageProperties properties, ImageOrphanRecorder orphanRecorder) {
        this(storage, validation, assetManager, properties, orphanRecorder, null);
    }

    public ImageIngestService(ImageStorageService storage, ImageValidationService validation,
        ImageAssetManager assetManager, ImageProperties properties, ImageOrphanRecorder orphanRecorder,
        ImageDomainMetrics metrics) {
        this(storage, validation, assetManager, properties, orphanRecorder, metrics, null);
    }

    @Autowired
    public ImageIngestService(ImageStorageService storage, ImageValidationService validation,
        ImageAssetManager assetManager, ImageProperties properties, ImageOrphanRecorder orphanRecorder,
        ImageDomainMetrics metrics, IdempotencyMetrics idempotencyMetrics) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.assetManager = Objects.requireNonNull(assetManager, "assetManager");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.orphanRecorder = orphanRecorder;
        this.metrics = metrics;
        this.idempotencyMetrics = idempotencyMetrics;
    }

    public ImageAssetDTO ingestUpload(ImageIngestCommand command, InputStream content) throws IOException {
        Assert.notNull(command, "command不能为空");
        Assert.notNull(content, "content不能为空");
        Assert.hasText(command.ownerType(), "ownerType不能为空");
        Assert.hasText(command.ownerId(), "ownerId不能为空");
        IdempotencyKeyValidator.validateOptional(command.idempotencyKey());
        validation.sanitizeOptionalFilename(command.originalFilename());
        validation.sanitizeOptionalFilename(command.assetName());
        String assetId = ImageConstant.ASSET_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
        StagedImage staged = null;
        String promotedKey = null;
        try {
            long stageStarted = System.nanoTime();
            try {
                staged = storage.stage(new ImageStageCommand(assetId, properties.getStorage().getWorkerNode(),
                    properties.getStorage().getMaxUploadBytes()), content);
            } finally {
                if (metrics != null) metrics.storage(Duration.ofNanos(System.nanoTime() - stageStarted), "STAGE", "LOCAL");
            }
            VerifiedImage verified;
            try (InputStream stagedContent = storage.openStaged(staged.stagingKey())) {
                verified = validation.inspect(stagedContent, staged.fileSize(), command.declaredContentType(),
                    properties.getStorage().getMaxUploadBytes(), properties.getCollection().getMaxPixels());
            }
            ImageUploadSnapshot requested = ImageUploadSnapshot.from(command, staged.checksum(), validation);
            if (StringUtils.hasText(command.idempotencyKey())) {
                ImageAssetDTO existing = assetManager.findByIdempotency(command.ownerType(), command.ownerId(),
                    command.idempotencyKey());
                if (existing != null) {
                    return replayOrConflict(requested, existing, assetManager.getSourceByAssetId(existing.getAssetId()));
                }
            }
            LocalDateTime capturedAt = LocalDateTime.now();
            String finalKey = ImageFinalKeyGenerator.generate(assetId, capturedAt, verified.format());
            long promoteStarted = System.nanoTime();
            StoredImage stored;
            try {
                stored = storage.promote(new ImagePromoteCommand(staged.stagingKey(), finalKey));
            } finally {
                    if (metrics != null) metrics.storage(Duration.ofNanos(System.nanoTime() - promoteStarted), "PROMOTE", "LOCAL");
            }
            promotedKey = stored.storageKey();
            ImageAssetDTO asset = new ImageAssetDTO();
            asset.setAssetId(assetId);
            asset.setAssetName(requested.getAssetName());
            asset.setStorageProvider(ImageStorageProviderEnum.LOCAL.name());
            asset.setStorageKey(promotedKey);
            asset.setContentType(verified.contentType());
            asset.setImageFormat(verified.format().name());
            asset.setFileSize(verified.fileSize());
            asset.setWidth(verified.width());
            asset.setHeight(verified.height());
            asset.setChecksumAlgorithm(ImageConstant.CHECKSUM_ALGORITHM);
            asset.setChecksum(staged.checksum());
            asset.setCapturedAt(capturedAt);
            asset.setOwnerType(command.ownerType());
            asset.setOwnerId(command.ownerId());
            asset.setOrganizationId(command.organizationId());
            asset.setIdempotencyKey(command.idempotencyKey());
            asset.setRetentionPolicy(ImageConstant.RETENTION_PERMANENT);
            ImageAssetSourceDTO source = new ImageAssetSourceDTO();
            source.setAssetId(assetId);
            source.setSourceType(ImageAssetSourceTypeEnum.USER_UPLOAD.name());
            source.setSourceSystem("VOGLANDER_WEB");
            source.setSourceEntityType("USER");
            source.setSourceEntityId(command.ownerId());
            source.setOriginalFilename(requested.getOriginalFilename());
            JSONObject metadata = new JSONObject();
            metadata.put("contentType", verified.contentType());
            metadata.put("imageFormat", verified.format().name());
            source.setSourceMetadata(metadata);
            try {
                ImageAssetCreateResultDTO result = assetManager.createWithSource(asset, source);
                if (!result.isCreated()) {
                    compensateFinal(promotedKey, "INGEST_RACE_DELETE_FALSE", "INGEST_RACE_DELETE_FAILED");
                    promotedKey = null;
                    return replayOrConflict(requested, result.getAcceptedAsset(), result.getAcceptedSource());
                }
                promotedKey = null;
                if (metrics != null) metrics.handler("SUCCESS", null);
                if (idempotencyMetrics != null && StringUtils.hasText(command.idempotencyKey())) {
                    idempotencyMetrics.record("CREATED", null);
                }
                return result.getAcceptedAsset();
            } catch (RuntimeException exception) {
                if (promotedKey != null) {
                    compensateFinal(promotedKey, "INGEST_COMPENSATION_DELETE_FALSE",
                        "INGEST_COMPENSATION_DELETE_FAILED");
                    promotedKey = null;
                }
                throw exception;
            }
        } catch (ServiceException exception) {
            if (metrics != null) metrics.handler("FAILED", String.valueOf(exception.getCode()));
            throw exception;
        } catch (IOException exception) {
            if (metrics != null) metrics.handler("FAILED", "IO_ERROR");
            throw exception;
        } catch (RuntimeException exception) {
            if (metrics != null) metrics.handler("FAILED", "RUNTIME_ERROR");
            throw new ServiceException(ServiceExceptionEnum.IMAGE_STORAGE_WRITE_FAILED)
                .setDetailMessage(exception.getClass().getSimpleName());
        } finally {
            if (staged != null) {
                try {
                    storage.discardStaged(staged.stagingKey());
                } catch (Exception cleanupException) {
                    log.error("Image ingest staging cleanup failed; cleanupCode={}",
                        ImageLogSanitizer.code(cleanupException.getClass().getSimpleName()));
                    if (orphanRecorder != null) {
                        orphanRecorder.record("INGEST_STAGING_DISCARD_FAILED", staged.stagingKey());
                    }
                    recordCompensationFailure();
                }
            }
        }
    }

    private ImageAssetDTO replayOrConflict(ImageUploadSnapshot requested, ImageAssetDTO acceptedAsset,
        ImageAssetSourceDTO acceptedSource) {
        if (acceptedAsset == null || acceptedSource == null) {
            throw new IllegalStateException("accepted image idempotency fact is incomplete");
        }
        ImageUploadSnapshot accepted = ImageUploadSnapshot.from(acceptedAsset, acceptedSource);
        if (!requested.fingerprint().equals(accepted.fingerprint())) {
            if (idempotencyMetrics != null) idempotencyMetrics.record("CONFLICT", "600007");
            throw new ServiceException(ServiceExceptionEnum.IDEMPOTENCY_KEY_REUSED);
        }
        if (idempotencyMetrics != null) idempotencyMetrics.record("REPLAYED", null);
        return acceptedAsset;
    }

    private void compensateFinal(String storageKey, String falseCode, String exceptionCode) {
        try {
            if (!storage.delete(storageKey)) {
                if (orphanRecorder != null) orphanRecorder.record(falseCode, storageKey);
                recordCompensationFailure();
            }
        } catch (Exception cleanupException) {
            log.error("Image ingest final cleanup failed; cleanupCode={}",
                ImageLogSanitizer.code(cleanupException.getClass().getSimpleName()));
            if (orphanRecorder != null) {
                orphanRecorder.record(exceptionCode, storageKey);
            }
            recordCompensationFailure();
        }
    }

    private void recordCompensationFailure() {
        if (idempotencyMetrics != null) idempotencyMetrics.record("COMPENSATION_FAILURE", "710008");
    }
}
