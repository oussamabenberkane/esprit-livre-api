package com.oussamabenberkane.espritlivre.service;

import com.luciad.imageio.webp.WebPWriteParam;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Service for converting images to WebP format.
 * Uses the sejda webp-imageio library with native libwebp bindings.
 */
@Service
public class ImageConversionService {

    private static final Logger LOG = LoggerFactory.getLogger(ImageConversionService.class);

    private static final float DEFAULT_QUALITY = 0.82f;

    @PostConstruct
    public void init() {
        // Verify WebP support is available at startup
        boolean webpSupported = isWebPWriterAvailable();
        if (webpSupported) {
            LOG.info("WebP image conversion support is available");
        } else {
            LOG.error("WebP image conversion support is NOT available!");
            LOG.error("Available ImageIO writers: {}", Arrays.toString(ImageIO.getWriterFormatNames()));
            LOG.error("Ensure webp-imageio library and native dependencies are properly installed");
        }
    }

    /**
     * Check if WebP ImageWriter is available.
     *
     * @return true if WebP writing is supported
     */
    public boolean isWebPWriterAvailable() {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        return writers.hasNext();
    }

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
        // Ensure image is in a compatible format for WebP conversion
        BufferedImage convertedImage = ensureCompatibleImage(image);

        // Find WebP writer by format name
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (!writers.hasNext()) {
            // Try by MIME type as fallback
            writers = ImageIO.getImageWritersByMIMEType("image/webp");
        }
        if (!writers.hasNext()) {
            LOG.error("No WebP ImageWriter found. Available writers: {}", java.util.Arrays.toString(ImageIO.getWriterFormatNames()));
            throw new IOException("No WebP ImageWriter found. Ensure webp-imageio library is on classpath.");
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

            writer.setOutput(ios);

            // Configure WebP write parameters
            WebPWriteParam writeParam = new WebPWriteParam(writer.getLocale());
            writeParam.setCompressionMode(WebPWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType(writeParam.getCompressionTypes()[0]); // "Lossy"
            writeParam.setCompressionQuality(quality);

            writer.write(null, new IIOImage(convertedImage, null, null), writeParam);
            ios.flush();

            LOG.debug("Image converted to WebP successfully. Quality: {}%, Size: {} bytes",
                (int) (quality * 100), baos.size());
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
     * WebP requires RGB format without alpha for lossy compression.
     *
     * @param original the original BufferedImage
     * @return a compatible BufferedImage in RGB format
     */
    private BufferedImage ensureCompatibleImage(BufferedImage original) {
        // If already in compatible format, return as-is
        if (original.getType() == BufferedImage.TYPE_INT_RGB ||
            original.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return original;
        }

        // Convert to RGB, filling transparent areas with white
        BufferedImage converted = new BufferedImage(
            original.getWidth(),
            original.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = converted.createGraphics();
        try {
            // Fill with white background for transparent images
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, converted.getWidth(), converted.getHeight());
            // Draw the original image on top
            g2d.drawImage(original, 0, 0, null);
        } finally {
            g2d.dispose();
        }

        return converted;
    }
}
