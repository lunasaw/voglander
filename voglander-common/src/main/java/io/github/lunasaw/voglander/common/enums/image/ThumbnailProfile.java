package io.github.lunasaw.voglander.common.enums.image;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

/** Fixed, allowlisted thumbnail profiles exposed by the private image API. */
public enum ThumbnailProfile {
    TABLE("table", 112, 84, 64 * 1024),
    GALLERY("gallery", 320, 240, 256 * 1024);

    private final String value;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxBytes;

    ThumbnailProfile(String value, int maxWidth, int maxHeight, int maxBytes) {
        this.value = value;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxBytes = maxBytes;
    }

    public static ThumbnailProfile parse(String value) {
        for (ThumbnailProfile profile : values()) {
            if (profile.value.equals(value)) {
                return profile;
            }
        }
        throw new ServiceException(ServiceExceptionEnum.IMAGE_THUMBNAIL_PROFILE_INVALID);
    }

    public String getValue() {
        return value;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getMaxBytes() {
        return maxBytes;
    }
}
