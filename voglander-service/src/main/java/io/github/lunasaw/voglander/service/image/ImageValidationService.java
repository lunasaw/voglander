package io.github.lunasaw.voglander.service.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import io.github.lunasaw.voglander.common.enums.image.ImageFormatEnum;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

/** Validates image signatures, decodability, dimensions and server-controlled MIME values. */
@Service
@ConditionalOnProperty(prefix = "voglander.image", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ImageValidationService {

    public VerifiedImage inspect(InputStream input, long fileSize, String declaredContentType, long maxBytes,
        long maxPixels) throws IOException {
        if (fileSize <= 0 || fileSize > maxBytes) {
            throw new ServiceException(ServiceExceptionEnum.IMAGE_FILE_TOO_LARGE);
        }
        if (maxPixels <= 0) {
            throw new IllegalArgumentException("maxPixels must be positive");
        }
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new ServiceException(ServiceExceptionEnum.IMAGE_DECODE_FAILED);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new ServiceException(ServiceExceptionEnum.IMAGE_FILE_TYPE_UNSUPPORTED);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                ImageFormatEnum format = ImageFormatEnum.fromReaderFormat(reader.getFormatName());
                if (format == null) {
                    throw new ServiceException(ServiceExceptionEnum.IMAGE_FILE_TYPE_UNSUPPORTED);
                }
                if (declaredContentType != null && !declaredContentType.isBlank()
                    && !format.getContentType().equalsIgnoreCase(declaredContentType)) {
                    throw new ServiceException(ServiceExceptionEnum.IMAGE_FILE_TYPE_UNSUPPORTED);
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || Math.multiplyExact((long)width, (long)height) > maxPixels) {
                    throw new ServiceException(ServiceExceptionEnum.IMAGE_PIXEL_LIMIT_EXCEEDED);
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new ServiceException(ServiceExceptionEnum.IMAGE_DECODE_FAILED);
                }
                return new VerifiedImage(format, format.getContentType(), fileSize, width, height);
            } catch (ServiceException exception) {
                throw exception;
            } catch (ArithmeticException exception) {
                throw new ServiceException(ServiceExceptionEnum.IMAGE_PIXEL_LIMIT_EXCEEDED);
            } catch (IOException | RuntimeException exception) {
                throw new ServiceException(ServiceExceptionEnum.IMAGE_DECODE_FAILED)
                    .setDetailMessage(exception.getClass().getSimpleName());
            } finally {
                reader.dispose();
            }
        }
    }

    /** Removes path/header control characters while retaining a useful display basename. */
    public String sanitizeFilename(String filename, String fallback) {
        String value = filename == null ? "" : filename.replace('\\', '/');
        int slash = value.lastIndexOf('/');
        value = slash >= 0 ? value.substring(slash + 1) : value;
        value = Normalizer.normalize(value, Normalizer.Form.NFC).replaceAll("[\\p{Cntrl}]", "").trim();
        if (value.isEmpty()) {
            value = fallback == null ? "image" : fallback;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
