package io.github.lunasaw.voglander.service.image;

/** Stable, non-sensitive reconciliation counters for health/metrics endpoints. */
public final class ImageStorageReconciliationReport {
    private final int expiredStaging;
    private final int unregisteredObjects;
    private final int missingAssets;

    public ImageStorageReconciliationReport(int expiredStaging, int unregisteredObjects, int missingAssets) {
        this.expiredStaging = expiredStaging;
        this.unregisteredObjects = unregisteredObjects;
        this.missingAssets = missingAssets;
    }

    public int expiredStaging() { return expiredStaging; }
    public int unregisteredObjects() { return unregisteredObjects; }
    public int missingAssets() { return missingAssets; }
}
