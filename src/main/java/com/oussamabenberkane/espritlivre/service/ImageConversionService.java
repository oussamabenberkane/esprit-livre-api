package com.oussamabenberkane.espritlivre.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Service for converting images to WebP format.
 */
@Service
public class ImageConversionService {

    private static final Logger LOG = LoggerFactory.getLogger(ImageConversionService.class);

    private static final float DEFAULT_QUALITY = 0.82f;

    /**
     * Convert an image from an input stream to WebP format.
     *
     * @param inputStream the input stream containing the image data
     * @return byte array containing the WebP image data
     * @throws IOException if conversion fails
     */
    public byte[] convertToWebP(InputStream inputStream) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IOException("Unable to read image from input stream");
        }
        return convertToWebP(image, DEFAULT_QUALITY);
    }

    /**
     * Convert a BufferedImage to WebP format with specified quality.
     *
     * @param image the BufferedImage to convert
     * @param quality the compression quality (0.0 to 1.0)
     * @return byte array containing the WebP image data
     * @throws IOException if conversion fails
     */
    public byte[] convertToWebP(BufferedImage image, float quality) throws IOException {
        // Ensure image has no alpha channel issues by converting to RGB if needed
        BufferedImage convertedImage = ensureCompatibleImage(image);

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IOException("No WebP ImageWriter found. Ensure imageio-webp library is on classpath.");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionType(param.getCompressionTypes()[0]);
                param.setCompressionQuality(quality);
            }

            writer.write(null, new IIOImage(convertedImage, null, null), param);
            ios.flush();

            LOG.debug("Image converted to WebP successfully. Quality: {}%", (int) (quality * 100));
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    /**
     * Check if the content type is already WebP.
     *
     * @param contentType the content type to check
     * @return true if already WebP format
     */
    public boolean isWebP(String contentType) {
        return contentType != null && contentType.toLowerCase().equals("image/webp");
    }

    /**
     * Ensure the image is in a compatible format for WebP conversion.
     * Converts ARGB images to RGB to avoid issues with transparency.
     *
     * @param original the original BufferedImage
     * @return a compatible BufferedImage
     */
    private BufferedImage ensureCompatibleImage(BufferedImage original) {
        if (original.getType() == BufferedImage.TYPE_INT_RGB ||
            original.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return original;
        }

        // Convert to RGB
        BufferedImage converted = new BufferedImage(
            original.getWidth(),
            original.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );
        converted.createGraphics().drawImage(original, 0, 0, null);
        return converted;
    }
}
