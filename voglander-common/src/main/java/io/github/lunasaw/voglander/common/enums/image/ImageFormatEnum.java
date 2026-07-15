package io.github.lunasaw.voglander.common.enums.image;

import java.util.Arrays;

/** Server-verified image formats. */
public enum ImageFormatEnum {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String contentType;
    private final String extension;

    ImageFormatEnum(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    public static ImageFormatEnum fromReaderFormat(String value) {
        if (value == null) {
            return null;
        }
        String normalized = "JPG".equalsIgnoreCase(value) ? "JPEG" : value;
        return Arrays.stream(values())
            .filter(format -> format.name().equalsIgnoreCase(normalized))
            .findFirst()
            .orElse(null);
    }
}
