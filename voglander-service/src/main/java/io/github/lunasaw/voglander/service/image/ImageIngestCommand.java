package io.github.lunasaw.voglander.service.image;

/** Trusted actor facts for an upload; owner is never taken from multipart fields. */
public final class ImageIngestCommand {
    private final String ownerType;
    private final String ownerId;
    private final String organizationId;
    private final String idempotencyKey;
    private final String originalFilename;
    private final String declaredContentType;
    private final String assetName;

    public ImageIngestCommand(String ownerType, String ownerId, String organizationId, String idempotencyKey,
        String originalFilename, String declaredContentType, String assetName) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.organizationId = organizationId;
        this.idempotencyKey = idempotencyKey;
        this.originalFilename = originalFilename;
        this.declaredContentType = declaredContentType;
        this.assetName = assetName;
    }

    public String ownerType() { return ownerType; }
    public String ownerId() { return ownerId; }
    public String organizationId() { return organizationId; }
    public String idempotencyKey() { return idempotencyKey; }
    public String originalFilename() { return originalFilename; }
    public String declaredContentType() { return declaredContentType; }
    public String assetName() { return assetName; }
}
