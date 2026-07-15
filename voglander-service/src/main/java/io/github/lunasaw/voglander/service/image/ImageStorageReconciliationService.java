package io.github.lunasaw.voglander.service.image;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.github.lunasaw.voglander.common.anno.TechnicalScheduler;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;
import io.github.lunasaw.voglander.intergration.wrapper.image.config.ImageProperties;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetQueryDTO;
import io.github.lunasaw.voglander.manager.manager.ImageAssetManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Provider/DB inventory reconciliation. Permanent objects are report-only by
 * default; registered assets are never deleted by this process.
 */
@Service
@Slf4j
@TechnicalScheduler(category = TechnicalScheduler.Category.MAINTENANCE)
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImageStorageReconciliationService {
    private final ImageStorageService storage;
    private final ImageAssetManager assetManager;
    private final ImageProperties properties;
    private final ImageDomainMetrics metrics;
    private volatile ImageStorageReconciliationReport lastReport = new ImageStorageReconciliationReport(0, 0, 0);

    public ImageStorageReconciliationService(ImageStorageService storage, ImageAssetManager assetManager,
        ImageProperties properties) {
        this(storage, assetManager, properties, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ImageStorageReconciliationService(ImageStorageService storage, ImageAssetManager assetManager,
        ImageProperties properties, ImageDomainMetrics metrics) {
        this.storage = storage;
        this.assetManager = assetManager;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${voglander.image.storage.reconciliation-interval-ms:3600000}")
    public void scheduledReconcile() {
        try {
            reconcile();
        } catch (Exception exception) {
            log.error("Image storage reconciliation failed: type={}", exception.getClass().getSimpleName());
        }
    }

    public ImageStorageReconciliationReport reconcile() throws IOException {
        int staging = storage.sweepStaging(properties.getStorage().getStagingTtl());
        Set<String> registered = new HashSet<>();
        ImageAssetQueryDTO query = new ImageAssetQueryDTO();
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ImageAssetDTO> page = assetManager.getPage(query, 1, 1000);
        for (ImageAssetDTO asset : page.getRecords()) {
            if (asset != null && asset.getStorageKey() != null) registered.add(asset.getStorageKey());
        }
        int unregistered = 0;
        for (String key : storage.listFinalKeys()) {
            if (!registered.contains(key)) {
                unregistered++;
                if (!properties.getStorage().isReconciliationReportOnly()) {
                    try { storage.delete(key); } catch (IOException exception) {
                        log.warn("Image unregistered object cleanup failed: kind=unregistered");
                    }
                }
            }
        }
        int missing = 0;
        for (String key : registered) {
            if (!storage.exists(key)) missing++;
        }
        lastReport = new ImageStorageReconciliationReport(staging, unregistered, missing);
        if (metrics != null) {
            metrics.reconciliation("STAGING", staging);
            metrics.reconciliation("UNREGISTERED", unregistered);
            metrics.reconciliation("MISSING", missing);
        }
        return lastReport;
    }

    public ImageStorageReconciliationReport lastReport() {
        return lastReport;
    }
}
