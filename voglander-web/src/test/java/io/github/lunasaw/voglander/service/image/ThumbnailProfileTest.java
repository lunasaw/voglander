package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.enums.image.ThumbnailProfile;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

class ThumbnailProfileTest {

    @Test
    void profilesExposeFixedDimensionsAndByteLimits() {
        assertEquals(112, ThumbnailProfile.parse("table").getMaxWidth());
        assertEquals(84, ThumbnailProfile.parse("table").getMaxHeight());
        assertEquals(64 * 1024, ThumbnailProfile.parse("table").getMaxBytes());
        assertEquals(320, ThumbnailProfile.parse("gallery").getMaxWidth());
        assertEquals(240, ThumbnailProfile.parse("gallery").getMaxHeight());
        assertEquals(256 * 1024, ThumbnailProfile.parse("gallery").getMaxBytes());
    }

    @Test
    void profileParsingIsExactAndRejectsMissingOrUnknownValues() {
        for (String invalid : new String[] {null, "", "TABLE", " gallery ", "large"}) {
            ServiceException error = assertThrows(ServiceException.class,
                () -> ThumbnailProfile.parse(invalid));
            assertEquals(ServiceExceptionEnum.IMAGE_THUMBNAIL_PROFILE_INVALID.getCode(), error.getCode());
        }
    }
}
