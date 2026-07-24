package io.github.lunasaw.voglander.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.common.enums.image.ThumbnailProfile;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

class ImageThumbnailTransformerTest {

    private final ImageThumbnailTransformer transformer = new ImageThumbnailTransformer();

    @Test
    void transparentPortraitIsCroppedToFourByThreeAndEncodedAsBoundedJpeg() throws Exception {
        BufferedImage source = new BufferedImage(240, 480, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(new Color(220, 30, 40, 160));
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        byte[] input = encode(source, "png");

        byte[] output = transformer.transform(new ImageContent(new ByteArrayInputStream(input), input.length),
            ThumbnailProfile.TABLE, 8_000_000L);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output));

        assertEquals(112, decoded.getWidth());
        assertEquals(84, decoded.getHeight());
        assertTrue(output.length <= ThumbnailProfile.TABLE.getMaxBytes());
        assertEquals(3, decoded.getColorModel().getNumColorComponents());
    }

    @Test
    void smallInputIsNeverUpscaled() throws Exception {
        BufferedImage source = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        byte[] input = encode(source, "jpg");

        byte[] output = transformer.transform(new ImageContent(new ByteArrayInputStream(input), input.length),
            ThumbnailProfile.GALLERY, 8_000_000L);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output));

        assertEquals(53, decoded.getWidth());
        assertEquals(40, decoded.getHeight());
    }

    @Test
    void corruptInputNeverFallsBackToOriginalBytes() {
        byte[] corrupt = new byte[] {1, 2, 3, 4};
        ServiceException error = assertThrows(ServiceException.class,
            () -> transformer.transform(new ImageContent(new ByteArrayInputStream(corrupt), corrupt.length),
                ThumbnailProfile.TABLE, 8_000_000L));
        assertEquals(ServiceExceptionEnum.IMAGE_THUMBNAIL_UNAVAILABLE.getCode(), error.getCode());
    }

    @ParameterizedTest
    @CsvSource({"1,RED", "3,YELLOW", "6,BLUE", "8,GREEN"})
    void jpegExifOrientationIsAppliedBeforeCenterCrop(int orientation, String expectedCorner) throws Exception {
        BufferedImage source = new BufferedImage(160, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 80, 60);
        graphics.setColor(Color.GREEN);
        graphics.fillRect(80, 0, 80, 60);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 60, 80, 60);
        graphics.setColor(Color.YELLOW);
        graphics.fillRect(80, 60, 80, 60);
        graphics.dispose();
        byte[] input = withExifOrientation(encode(source, "jpg"), orientation);

        byte[] output = transformer.transform(new ImageContent(new ByteArrayInputStream(input), input.length),
            ThumbnailProfile.TABLE, 8_000_000L);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(output));

        assertEquals(expectedCorner, classify(decoded.getRGB(10, 10)));
    }

    private byte[] encode(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private byte[] withExifOrientation(byte[] jpeg, int orientation) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream(jpeg.length + 36);
        output.write(jpeg, 0, 2);
        output.write(new byte[] {
            (byte)0xff, (byte)0xe1, 0x00, 0x22,
            'E', 'x', 'i', 'f', 0x00, 0x00,
            'I', 'I', 0x2a, 0x00, 0x08, 0x00, 0x00, 0x00,
            0x01, 0x00,
            0x12, 0x01, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00,
            (byte)orientation, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        });
        output.write(jpeg, 2, jpeg.length - 2);
        return output.toByteArray();
    }

    private String classify(int rgb) {
        Color color = new Color(rgb);
        if (color.getRed() > 180 && color.getGreen() > 180) return "YELLOW";
        if (color.getRed() > color.getGreen() * 2 && color.getRed() > color.getBlue() * 2) return "RED";
        if (color.getGreen() > color.getRed() * 2 && color.getGreen() > color.getBlue() * 2) return "GREEN";
        if (color.getBlue() > color.getRed() * 2 && color.getBlue() > color.getGreen() * 2) return "BLUE";
        return "UNKNOWN";
    }
}
