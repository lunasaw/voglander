package io.github.lunasaw.voglander.service.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.springframework.stereotype.Component;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.common.constant.image.ImageConstant;
import io.github.lunasaw.voglander.common.enums.image.ThumbnailProfile;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;

/** Safe ImageIO transformation for the two fixed thumbnail profiles. */
@Component
public class ImageThumbnailTransformer {

    private static final Color BACKGROUND = new Color(0xF2, 0xF0, 0xEA);
    private static final float[] JPEG_QUALITIES = new float[] {0.85f, 0.75f, 0.65f, 0.55f};

    public byte[] transform(ImageContent content, ThumbnailProfile profile, long maxWorkingPixels) {
        return transform(content, profile, maxWorkingPixels, ImageConstant.DEFAULT_MAX_FILE_SIZE,
            ImageConstant.DEFAULT_MAX_PIXELS);
    }

    public byte[] transform(ImageContent content, ThumbnailProfile profile, long maxWorkingPixels,
        long maxSourceBytes, long maxSourcePixels) {
        if (content == null || profile == null || maxWorkingPixels < profile.getMaxWidth() * profile.getMaxHeight()
            || maxSourceBytes <= 0 || maxSourcePixels <= 0 || content.contentLength() > maxSourceBytes) {
            throw unavailable();
        }
        ImageReader reader = null;
        try (ImageInputStream input = new MemoryCacheImageInputStream(content.inputStream())) {
            int orientation = exifOrientation(input);
            input.seek(0L);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw unavailable();
            reader = readers.next();
            reader.setInput(input, true, false);
            int sourceWidth = reader.getWidth(0);
            int sourceHeight = reader.getHeight(0);
            if (sourceWidth <= 0 || sourceHeight <= 0
                || (long)sourceWidth * sourceHeight > maxSourcePixels) throw unavailable();
            if (orientation == 0) orientation = orientation(reader);
            ImageReadParam readParam = reader.getDefaultReadParam();
            int subsampling = subsampling(sourceWidth, sourceHeight, profile, maxWorkingPixels);
            if (subsampling > 1) readParam.setSourceSubsampling(subsampling, subsampling, 0, 0);
            BufferedImage decoded = reader.read(0, readParam);
            if (decoded == null || (long)decoded.getWidth() * decoded.getHeight() > maxWorkingPixels) {
                throw unavailable();
            }
            BufferedImage oriented = orient(decoded, orientation);
            BufferedImage cropped = cropFourByThree(oriented);
            BufferedImage scaled = scaleDown(cropped, profile);
            BufferedImage rgb = flatten(scaled);
            for (float quality : JPEG_QUALITIES) {
                byte[] encoded = encodeJpeg(rgb, quality);
                if (encoded.length <= profile.getMaxBytes()) return encoded;
            }
            throw unavailable();
        } catch (ServiceException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unavailable();
        } finally {
            if (reader != null) reader.dispose();
        }
    }

    private int subsampling(int width, int height, ThumbnailProfile profile, long maxWorkingPixels) {
        long pixels = (long)width * height;
        int required = pixels <= maxWorkingPixels ? 1
            : (int)Math.ceil(Math.sqrt((double)pixels / maxWorkingPixels));
        int target = Math.max(1, Math.min(width / Math.max(1, profile.getMaxWidth() * 2),
            height / Math.max(1, profile.getMaxHeight() * 2)));
        return Math.max(required, target);
    }

    private int orientation(ImageReader reader) {
        try {
            IIOMetadata metadata = reader.getImageMetadata(0);
            if (metadata == null) return 1;
            for (String format : metadata.getMetadataFormatNames()) {
                Integer value = findOrientation(metadata.getAsTree(format));
                if (value != null) return value.intValue();
            }
        } catch (Exception ignored) {
            // Orientation is optional; malformed metadata must not break an otherwise decodable image.
        }
        return 1;
    }

    private int exifOrientation(ImageInputStream input) {
        try {
            input.seek(0L);
            if (input.readUnsignedShort() != 0xffd8) return 0;
            for (int segment = 0; segment < 64; segment++) {
                int prefix;
                do {
                    prefix = input.readUnsignedByte();
                } while (prefix != 0xff);
                int marker;
                do {
                    marker = input.readUnsignedByte();
                } while (marker == 0xff);
                if (marker == 0xd9 || marker == 0xda) return 0;
                if (marker == 0x01 || marker >= 0xd0 && marker <= 0xd7) continue;
                int length = input.readUnsignedShort();
                if (length < 2) return 0;
                int payloadLength = length - 2;
                if (marker == 0xe1 && payloadLength >= 14) {
                    byte[] payload = new byte[payloadLength];
                    input.readFully(payload);
                    int orientation = parseExifOrientation(payload);
                    if (orientation != 0) return orientation;
                } else {
                    input.seek(input.getStreamPosition() + payloadLength);
                }
            }
        } catch (Exception ignored) {
            // Missing or malformed EXIF is equivalent to the normal orientation.
        }
        return 0;
    }

    private int parseExifOrientation(byte[] payload) {
        if (payload.length < 20 || payload[0] != 'E' || payload[1] != 'x' || payload[2] != 'i'
            || payload[3] != 'f' || payload[4] != 0 || payload[5] != 0) return 0;
        int tiff = 6;
        boolean littleEndian;
        if (payload[tiff] == 'I' && payload[tiff + 1] == 'I') littleEndian = true;
        else if (payload[tiff] == 'M' && payload[tiff + 1] == 'M') littleEndian = false;
        else return 0;
        if (unsignedShort(payload, tiff + 2, littleEndian) != 42) return 0;
        long offset = unsignedInt(payload, tiff + 4, littleEndian);
        if (offset > Integer.MAX_VALUE) return 0;
        int directory = tiff + (int)offset;
        if (directory < tiff || directory + 2 > payload.length) return 0;
        int count = unsignedShort(payload, directory, littleEndian);
        for (int index = 0; index < count; index++) {
            int entry = directory + 2 + index * 12;
            if (entry < 0 || entry + 12 > payload.length) return 0;
            int tag = unsignedShort(payload, entry, littleEndian);
            if (tag != 0x0112) continue;
            if (unsignedShort(payload, entry + 2, littleEndian) != 3
                || unsignedInt(payload, entry + 4, littleEndian) < 1) return 0;
            int value = unsignedShort(payload, entry + 8, littleEndian);
            return value == 1 || value == 3 || value == 6 || value == 8 ? value : 1;
        }
        return 0;
    }

    private int unsignedShort(byte[] value, int offset, boolean littleEndian) {
        if (offset < 0 || offset + 2 > value.length) return -1;
        int first = value[offset] & 0xff;
        int second = value[offset + 1] & 0xff;
        return littleEndian ? first | second << 8 : first << 8 | second;
    }

    private long unsignedInt(byte[] value, int offset, boolean littleEndian) {
        if (offset < 0 || offset + 4 > value.length) return -1L;
        long first = value[offset] & 0xffL;
        long second = value[offset + 1] & 0xffL;
        long third = value[offset + 2] & 0xffL;
        long fourth = value[offset + 3] & 0xffL;
        return littleEndian ? first | second << 8 | third << 16 | fourth << 24
            : first << 24 | second << 16 | third << 8 | fourth;
    }

    private Integer findOrientation(Node node) {
        if (node == null) return null;
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            Node number = attributes.getNamedItem("number");
            if (number != null && "274".equals(number.getNodeValue())) {
                Integer nested = firstInteger(node);
                if (nested != null) return nested;
            }
            Node keyword = attributes.getNamedItem("keyword");
            Node value = attributes.getNamedItem("value");
            if (keyword != null && value != null && "Orientation".equalsIgnoreCase(keyword.getNodeValue())) {
                return parseInteger(value.getNodeValue());
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Integer found = findOrientation(child);
            if (found != null) return found;
        }
        return null;
    }

    private Integer firstInteger(Node node) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            Node value = attributes.getNamedItem("value");
            if (value != null) {
                Integer parsed = parseInteger(value.getNodeValue());
                if (parsed != null) return parsed;
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Integer parsed = firstInteger(child);
            if (parsed != null) return parsed;
        }
        return null;
    }

    private Integer parseInteger(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BufferedImage orient(BufferedImage source, int orientation) {
        if (orientation != 3 && orientation != 6 && orientation != 8) return source;
        int width = source.getWidth();
        int height = source.getHeight();
        int targetWidth = orientation == 6 || orientation == 8 ? height : width;
        int targetHeight = orientation == 6 || orientation == 8 ? width : height;
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        AffineTransform transform = new AffineTransform();
        if (orientation == 3) {
            transform.translate(width, height);
            transform.rotate(Math.PI);
        } else if (orientation == 6) {
            transform.translate(height, 0);
            transform.rotate(Math.PI / 2);
        } else {
            transform.translate(0, width);
            transform.rotate(-Math.PI / 2);
        }
        graphics.drawImage(source, transform, null);
        graphics.dispose();
        return target;
    }

    private BufferedImage cropFourByThree(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double ratio = (double)width / height;
        int cropWidth = width;
        int cropHeight = height;
        if (ratio > 4.0 / 3.0) cropWidth = Math.max(1, (int)Math.floor(height * 4.0 / 3.0));
        else if (ratio < 4.0 / 3.0) cropHeight = Math.max(1, (int)Math.floor(width * 3.0 / 4.0));
        int x = (width - cropWidth) / 2;
        int y = (height - cropHeight) / 2;
        BufferedImage target = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        graphics.drawImage(source, 0, 0, cropWidth, cropHeight, x, y, x + cropWidth, y + cropHeight, null);
        graphics.dispose();
        return target;
    }

    private BufferedImage scaleDown(BufferedImage source, ThumbnailProfile profile) {
        double scale = Math.min(1.0, Math.min((double)profile.getMaxWidth() / source.getWidth(),
            (double)profile.getMaxHeight() / source.getHeight()));
        int width = Math.max(1, (int)Math.floor(source.getWidth() * scale));
        int height = Math.max(1, (int)Math.floor(source.getHeight() * scale));
        if (width == source.getWidth() && height == source.getHeight()) return source;
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return target;
    }

    private BufferedImage flatten(BufferedImage source) {
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setColor(BACKGROUND);
        graphics.fillRect(0, 0, target.getWidth(), target.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return target;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw unavailable();
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam parameter = writer.getDefaultWriteParam();
            if (parameter.canWriteCompressed()) {
                parameter.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parameter.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), parameter);
            output.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private ServiceException unavailable() {
        return new ServiceException(ServiceExceptionEnum.IMAGE_THUMBNAIL_UNAVAILABLE);
    }
}
