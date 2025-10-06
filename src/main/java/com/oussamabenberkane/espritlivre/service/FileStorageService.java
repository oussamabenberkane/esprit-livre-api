package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * Service for handling file storage operations.
 */
@Service
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);

    private static final String UPLOAD_DIR = "src/main/resources/media/books";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    );
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");

    // Image dimension constraints (reasonable for book covers)
    private static final int MIN_WIDTH = 300;
    private static final int MIN_HEIGHT = 400;
    private static final int MAX_WIDTH = 2000;
    private static final int MAX_HEIGHT = 3000;

    /**
     * Store a book cover image.
     *
     * @param file the uploaded file
     * @param bookId the book ID
     * @return the relative URL path to the stored file
     * @throws IOException if file storage fails
     */
    public String storeBookCover(MultipartFile file, Long bookId) throws IOException {
        LOG.debug("Request to store book cover for book ID: {}", bookId);

        // Validate file
        validateFile(file);

        // Validate image dimensions
        validateImageDimensions(file);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate filename: cover_{bookId}.{extension}
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = "cover_" + bookId + "." + extension;

        // Store file
        Path targetLocation = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        LOG.debug("Book cover stored successfully: {}", filename);

        // Return relative URL path
        return "/media/books/" + filename;
    }

    /**
     * Delete a book cover image.
     *
     * @param coverImageUrl the relative URL path to the cover image
     */
    public void deleteBookCover(String coverImageUrl) {
        if (coverImageUrl == null || coverImageUrl.isEmpty()) {
            return;
        }

        try {
            // Extract filename from URL path
            String filename = coverImageUrl.substring(coverImageUrl.lastIndexOf('/') + 1);
            Path filePath = Paths.get(UPLOAD_DIR, filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                LOG.debug("Deleted book cover: {}", filename);
            }
        } catch (IOException e) {
            LOG.error("Failed to delete book cover: {}", coverImageUrl, e);
        }
    }

    /**
     * Validate the uploaded file.
     *
     * @param file the file to validate
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestAlertException("File is required", "book", "filerequired");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestAlertException(
                "File size exceeds maximum limit of 10 MB",
                "book",
                "filesizeexceeded"
            );
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestAlertException(
                "Invalid file type. Only JPEG, PNG, and WebP images are allowed",
                "book",
                "invalidfiletype"
            );
        }

        // Validate file extension
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestAlertException(
                "Invalid file extension. Only jpg, jpeg, png, and webp are allowed",
                "book",
                "invalidextension"
            );
        }
    }

    /**
     * Validate image dimensions.
     *
     * @param file the image file to validate
     * @throws IOException if image cannot be read
     */
    private void validateImageDimensions(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());

        if (image == null) {
            throw new BadRequestAlertException(
                "Unable to read image file. File may be corrupted",
                "book",
                "invalidimage"
            );
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width < MIN_WIDTH || height < MIN_HEIGHT) {
            throw new BadRequestAlertException(
                String.format("Image dimensions too small. Minimum size is %dx%d pixels", MIN_WIDTH, MIN_HEIGHT),
                "book",
                "imagetoosmall"
            );
        }

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            throw new BadRequestAlertException(
                String.format("Image dimensions too large. Maximum size is %dx%d pixels", MAX_WIDTH, MAX_HEIGHT),
                "book",
                "imagetoolarge"
            );
        }

        LOG.debug("Image dimensions validated: {}x{}", width, height);
    }

    /**
     * Get file extension from filename.
     *
     * @param filename the filename
     * @return the file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestAlertException("Invalid filename", "book", "invalidfilename");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
