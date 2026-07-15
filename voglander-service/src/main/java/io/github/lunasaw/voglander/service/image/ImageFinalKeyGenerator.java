package io.github.lunasaw.voglander.service.image;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.enums.image.ImageFormatEnum;

/** Generates provider-local final keys from server-owned asset identity and capture date. */
public final class ImageFinalKeyGenerator {
    private static final DateTimeFormatter PARTITION = DateTimeFormatter.ofPattern("uuuu/MM/dd");

    private ImageFinalKeyGenerator() {
    }

    public static String generate(String assetId, LocalDateTime capturedAt, ImageFormatEnum format) {
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(format, "format");
        if (assetId == null || !assetId.matches(ImageConstant.ASSET_ID_PREFIX + "[0-9a-f]{32}")) {
            throw new IllegalArgumentException("assetId must be a generated image id");
        }
        return "images/" + PARTITION.format(capturedAt) + "/" + assetId + "." + format.getExtension();
    }
}
