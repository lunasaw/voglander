package io.github.lunasaw.voglander.intergration.wrapper.image.config;

import java.time.Duration;
import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;

/**
 * Image-specific runtime configuration.  Task executor settings intentionally
 * live in the durable-task module and are not duplicated here.
 */
@ConfigurationProperties(prefix = "voglander.image")
public class ImageProperties {

    private boolean enabled = true;
    private Storage storage = new Storage();
    private Snapshot snapshot = new Snapshot();
    private Collection collection = new Collection();
    private Thumbnail thumbnail = new Thumbnail();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage == null ? new Storage() : storage;
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot == null ? new Snapshot() : snapshot;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection == null ? new Collection() : collection;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail == null ? new Thumbnail() : thumbnail;
    }

    public static class Storage {
        private String provider = "LOCAL";
        private String localRoot = Path.of(System.getProperty("user.home", "."), ".voglander", "images").toString();
        private String workerNode = "local";
        private long maxUploadBytes = ImageConstant.DEFAULT_MAX_FILE_SIZE;
        private Duration stagingTtl = Duration.ofHours(1);
        private boolean reconciliationReportOnly = true;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getLocalRoot() {
            return localRoot;
        }

        public void setLocalRoot(String localRoot) {
            this.localRoot = localRoot;
        }

        public String getWorkerNode() {
            return workerNode;
        }

        public void setWorkerNode(String workerNode) {
            this.workerNode = workerNode;
        }

        public long getMaxUploadBytes() {
            return maxUploadBytes;
        }

        public void setMaxUploadBytes(long maxUploadBytes) {
            this.maxUploadBytes = maxUploadBytes;
        }

        public Duration getStagingTtl() {
            return stagingTtl;
        }

        public void setStagingTtl(Duration stagingTtl) {
            this.stagingTtl = stagingTtl;
        }

        public boolean isReconciliationReportOnly() {
            return reconciliationReportOnly;
        }

        public void setReconciliationReportOnly(boolean reconciliationReportOnly) {
            this.reconciliationReportOnly = reconciliationReportOnly;
        }
    }

    public static class Snapshot {
        private int timeoutSeconds = 15;
        private int expireSeconds = 0;

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getExpireSeconds() {
            return expireSeconds;
        }

        public void setExpireSeconds(int expireSeconds) {
            this.expireSeconds = expireSeconds;
        }
    }

    public static class Collection {
        private long maxPixels = ImageConstant.DEFAULT_MAX_PIXELS;
        private int minIntervalSeconds = ImageConstant.DEFAULT_MIN_INTERVAL_SECONDS;
        private int maxPlannedCount = ImageConstant.DEFAULT_MAX_PLANNED_COUNT;

        public long getMaxPixels() {
            return maxPixels;
        }

        public void setMaxPixels(long maxPixels) {
            this.maxPixels = maxPixels;
        }

        public int getMinIntervalSeconds() {
            return minIntervalSeconds;
        }

        public void setMinIntervalSeconds(int minIntervalSeconds) {
            this.minIntervalSeconds = minIntervalSeconds;
        }

        public int getMaxPlannedCount() {
            return maxPlannedCount;
        }

        public void setMaxPlannedCount(int maxPlannedCount) {
            this.maxPlannedCount = maxPlannedCount;
        }
    }

    public static class Thumbnail {
        private boolean enabled = true;
        private String algorithmVersion = "thumb-v1";
        private int workerCount = Math.min(Runtime.getRuntime().availableProcessors(), 4);
        private int queueCapacity = 64;
        private long timeoutMillis = 3000;
        private long cacheMaxBytes = 64L * 1024 * 1024;
        private int cacheMaxEntries = 512;
        private long cacheTtlSeconds = 300;
        private long maxWorkingPixels = 8_000_000L;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAlgorithmVersion() { return algorithmVersion; }
        public void setAlgorithmVersion(String algorithmVersion) { this.algorithmVersion = algorithmVersion; }
        public int getWorkerCount() { return workerCount; }
        public void setWorkerCount(int workerCount) { this.workerCount = workerCount; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public long getTimeoutMillis() { return timeoutMillis; }
        public void setTimeoutMillis(long timeoutMillis) { this.timeoutMillis = timeoutMillis; }
        public long getCacheMaxBytes() { return cacheMaxBytes; }
        public void setCacheMaxBytes(long cacheMaxBytes) { this.cacheMaxBytes = cacheMaxBytes; }
        public int getCacheMaxEntries() { return cacheMaxEntries; }
        public void setCacheMaxEntries(int cacheMaxEntries) { this.cacheMaxEntries = cacheMaxEntries; }
        public long getCacheTtlSeconds() { return cacheTtlSeconds; }
        public void setCacheTtlSeconds(long cacheTtlSeconds) { this.cacheTtlSeconds = cacheTtlSeconds; }
        public long getMaxWorkingPixels() { return maxWorkingPixels; }
        public void setMaxWorkingPixels(long maxWorkingPixels) { this.maxWorkingPixels = maxWorkingPixels; }

        public void validate() {
            int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
            if (algorithmVersion == null || algorithmVersion.trim().isEmpty() || algorithmVersion.length() > 64
                || workerCount < 1 || workerCount > processors * 2
                || queueCapacity < 1 || queueCapacity > 1024
                || timeoutMillis < 100 || timeoutMillis > 30_000
                || cacheMaxBytes < 256L * 1024
                || cacheMaxEntries < 1 || cacheMaxEntries > 10_000
                || cacheTtlSeconds < 1 || cacheTtlSeconds > 3600
                || maxWorkingPixels < 320L * 240) {
                throw new IllegalStateException("Invalid voglander.image.thumbnail configuration");
            }
        }
    }
}
