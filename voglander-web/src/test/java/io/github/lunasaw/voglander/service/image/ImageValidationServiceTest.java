package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.common.exception.ServiceException;

class ImageValidationServiceTest {

    private final ImageValidationService service = new ImageValidationService();

    @Test
    void inspect_shouldDeriveFormatDimensionsAndMimeFromDecodedBytes() throws Exception {
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        VerifiedImage verified = service.inspect(new ByteArrayInputStream(output.toByteArray()), output.size(),
            "image/png", 1024 * 1024, 100);

        assertEquals("PNG", verified.format().name());
        assertEquals("image/png", verified.contentType());
        assertEquals(3, verified.width());
        assertEquals(2, verified.height());
    }

    @Test
    void inspect_shouldAcceptRealJpegPngAndWebpSamplesAndRejectOversizeBytes() throws Exception {
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        Map<String, String> formats = Map.of("jpeg", "image/jpeg", "png", "image/png");
        for (Map.Entry<String, String> entry : formats.entrySet()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            org.junit.jupiter.api.Assertions.assertTrue(ImageIO.write(image, entry.getKey(), output),
                "ImageIO writer missing for " + entry.getKey());
            byte[] bytes = output.toByteArray();
            VerifiedImage verified = service.inspect(new ByteArrayInputStream(bytes), bytes.length, entry.getValue(),
                1024 * 1024, 100);
            assertEquals(entry.getValue(), verified.contentType());
            assertEquals(4, verified.width());
            assertEquals(3, verified.height());
            assertThrows(ServiceException.class,
                () -> service.inspect(new ByteArrayInputStream(bytes), bytes.length, entry.getValue(), bytes.length - 1, 100));
        }
        // TwelveMonkeys provides a reader (not a writer); keep a real 1x1 WebP fixture in
        // the test so the server-side decoder is exercised without relying on a fake MIME.
        byte[] webp = Base64.getDecoder().decode("UklGRhIAAABXRUJQVlA4TAYAAAAvAAAAAAfQ//73v/+BiOh/AAA=");
        VerifiedImage webpVerified = service.inspect(new ByteArrayInputStream(webp), webp.length,
            "image/webp", 1024 * 1024, 100);
        assertEquals("WEBP", webpVerified.format().name());
        assertEquals(1, webpVerified.width());
        assertEquals(1, webpVerified.height());
        assertThrows(ServiceException.class,
            () -> service.inspect(new ByteArrayInputStream(webp), webp.length, "image/webp", webp.length - 1, 100));
    }

    @Test
    void inspect_shouldRejectMismatchedMimePixelLimitAndCorruptBytes() throws Exception {
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        byte[] bytes = output.toByteArray();

        assertThrows(ServiceException.class,
            () -> service.inspect(new ByteArrayInputStream(bytes), bytes.length, "image/jpeg", 1024 * 1024, 100));
        assertThrows(ServiceException.class,
            () -> service.inspect(new ByteArrayInputStream(bytes), bytes.length, "image/png", 1024 * 1024, 5));
        assertThrows(ServiceException.class,
            () -> service.inspect(new ByteArrayInputStream(new byte[] {1, 2, 3}), 3, null, 1024, 100));
    }

    @Test
    void sanitizeFilename_shouldRemovePathAndControlCharacters() {
        assertEquals("photo.jpg", service.sanitizeFilename("../photo.jpg\r\n", "fallback"));
        assertEquals("fallback", service.sanitizeFilename("../\r\n", "fallback"));
    }
}
