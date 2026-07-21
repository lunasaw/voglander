package io.github.lunasaw.voglander.common.constant.image;

/** Shared constants for the image asset and collection domain. */
public final class ImageConstant {

    public static final String ASSET_ID_PREFIX = "img_";
    public static final String TASK_TYPE_IMAGE_COLLECTION = "IMAGE_COLLECTION";
    public static final int TASK_PAYLOAD_VERSION = 1;

    public static final String PERMISSION_ASSET_QUERY = "Image:Asset:Query";
    public static final String PERMISSION_ASSET_VIEW = "Image:Asset:View";
    public static final String PERMISSION_ASSET_UPLOAD = "Image:Asset:Upload";
    public static final String PERMISSION_ASSET_DOWNLOAD = "Image:Asset:Download";
    public static final String PERMISSION_ASSET_DELETE = "Image:Asset:Delete";
    public static final String PERMISSION_COLLECTION_QUERY = "Image:Collection:Query";
    public static final String PERMISSION_COLLECTION_CREATE = "Image:Collection:Create";
    public static final String PERMISSION_COLLECTION_CONTROL = "Image:Collection:Control";

    public static final String SSE_ASSET_CREATED = "image.asset.created";
    public static final String SSE_ASSET_DELETED = "image.asset.deleted";
    public static final String LOCK_CAMERA_PREFIX = "image:collection:camera:";

    public static final long DEFAULT_MAX_FILE_SIZE = 20L * 1024 * 1024;
    public static final long DEFAULT_MAX_PIXELS = 40_000_000L;
    public static final int DEFAULT_MIN_INTERVAL_SECONDS = 30;
    public static final int DEFAULT_MAX_PLANNED_COUNT = 10_000;
    public static final String CHECKSUM_ALGORITHM = "SHA256";
    public static final String RETENTION_PERMANENT = "PERMANENT";

    private ImageConstant() {}
}
