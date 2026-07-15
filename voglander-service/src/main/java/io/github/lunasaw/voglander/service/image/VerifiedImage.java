package io.github.lunasaw.voglander.service.image;

import io.github.lunasaw.voglander.common.enums.image.ImageFormatEnum;

/** Server-derived image facts; request MIME and filename are never treated as authoritative. */
public final class VerifiedImage {
    private final ImageFormatEnum format;
    private final String contentType;
    private final long fileSize;
    private final int width;
    private final int height;

    public VerifiedImage(ImageFormatEnum format, String contentType, long fileSize, int width, int height) {
        if (format == null || contentType == null || fileSize <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("verified image facts are invalid");
        }
        this.format = format;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
    }

    public ImageFormatEnum format() { return format; }
    public String contentType() { return contentType; }
    public long fileSize() { return fileSize; }
    public int width() { return width; }
    public int height() { return height; }
}
