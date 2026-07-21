package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.enums.image.ImageFormatEnum;

class ImageFinalKeyGeneratorTest {
    @Test
    void generate_shouldPartitionByCaptureDateAndVerifiedExtension() {
        assertEquals("images/2026/07/15/img_0123456789abcdef0123456789abcdef.webp",
            ImageFinalKeyGenerator.generate("img_0123456789abcdef0123456789abcdef",
                LocalDateTime.of(2026, 7, 15, 23, 59, 59), ImageFormatEnum.WEBP));
    }

    @Test
    void generate_shouldRejectUntrustedAssetIdentityOrMissingFormat() {
        assertThrows(IllegalArgumentException.class,
            () -> ImageFinalKeyGenerator.generate("img_../escape", LocalDateTime.now(), ImageFormatEnum.JPEG));
        assertThrows(NullPointerException.class,
            () -> ImageFinalKeyGenerator.generate("img_0123456789abcdef0123456789abcdef", LocalDateTime.now(), null));
    }
}
