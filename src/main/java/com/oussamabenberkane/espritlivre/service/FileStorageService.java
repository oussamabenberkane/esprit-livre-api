package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
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

    private static final String MEDIA_ROOT_DIR = "src/main/resources/media";
    private static final String BOOKS_DIR = MEDIA_ROOT_DIR + "/books";
    private static final String BOOK_PACKS_DIR = MEDIA_ROOT_DIR + "/book-packs";
    private static final String AUTHORS_DIR = MEDIA_ROOT_DIR + "/authors";
    private static final String CATEGORIES_DIR = MEDIA_ROOT_DIR + "/categories";
    private static final String USERS_DIR = MEDIA_ROOT_DIR + "/users";

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
        return storeImage(file, BOOKS_DIR, "cover_" + bookId, "/media/books/", "book");
    }

    /**
     * Store a book pack cover image.
     *
     * @param file the uploaded file
     * @param bookPackId the book pack ID
     * @return the relative URL path to the stored file
     * @throws IOException if file storage fails
     */
    public String storeBookPackCover(MultipartFile file, Long bookPackId) throws IOException {
        LOG.debug("Request to store book pack cover for book pack ID: {}", bookPackId);
        return storeImage(file, BOOK_PACKS_DIR, "pack_" + bookPackId, "/media/book-packs/", "bookPack");
    }

    /**
     * Store an author profile picture.
     *
     * @param file the uploaded file
     * @param authorId the author ID
     * @return the relative URL path to the stored file
     * @throws IOException if file storage fails
     */
    public String storeAuthorPicture(MultipartFile file, Long authorId) throws IOException {
        LOG.debug("Request to store author picture for author ID: {}", authorId);
        return storeImage(file, AUTHORS_DIR, "author_" + authorId, "/media/authors/", "author");
    }

    /**
     * Store a category image.
     *
     * @param file the uploaded file
     * @param tagId the tag ID
     * @return the relative URL path to the stored file
     * @throws IOException if file storage fails
     */
    public String storeCategoryImage(MultipartFile file, Long tagId) throws IOException {
        LOG.debug("Request to store category image for tag ID: {}", tagId);
        return storeImage(file, CATEGORIES_DIR, "category_" + tagId, "/media/categories/", "tag");
    }

    /**
     * Store a user profile picture.
     *
     * @param file the uploaded file
     * @param userId the user ID
     * @return the relative URL path to the stored file
     * @throws IOException if file storage fails
     */
    public String storeUserPicture(MultipartFile file, String userId) throws IOException {
        LOG.debug("Request to store user picture for user ID: {}", userId);
        return storeImage(file, USERS_DIR, "user_" + userId, "/media/users/", "user");
    }

    /**
     * Generic method to store an image.
     *
     * @param file the uploaded file
     * @param uploadDir the directory to store the file in
     * @param filenamePrefix the prefix for the filename
     * @param urlPrefix the URL prefix for the returned path
     * @param entityName the entity name for error messages
     * @return the relative URL path to the stored file
     * @throws IOException if file storage fails
     */
    private String storeImage(MultipartFile file, String uploadDir, String filenamePrefix, String urlPrefix, String entityName) throws IOException {
        // Validate file
        validateFile(file, entityName);

        // Validate image dimensions
        validateImageDimensions(file, entityName);

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename, entityName);
        String filename = filenamePrefix + "." + extension;

        // Store file
        Path targetLocation = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        LOG.debug("Image stored successfully: {}", filename);

        // Return relative URL path
        return urlPrefix + filename;
    }



    /**
     * Delete a book cover image.
     *
     * @param coverImageUrl the relative URL path to the cover image
     */
    public void deleteBookCover(String coverImageUrl) {
        deleteImage(coverImageUrl, BOOKS_DIR);
    }

    /**
     * Delete a book pack cover image.
     *
     * @param coverImageUrl the relative URL path to the cover image
     */
    public void deleteBookPackCover(String coverImageUrl) {
        deleteImage(coverImageUrl, BOOK_PACKS_DIR);
    }

    /**
     * Delete an author profile picture.
     *
     * @param profilePictureUrl the relative URL path to the profile picture
     */
    public void deleteAuthorPicture(String profilePictureUrl) {
        deleteImage(profilePictureUrl, AUTHORS_DIR);
    }

    /**
     * Delete a category image.
     *
     * @param imageUrl the relative URL path to the category image
     */
    public void deleteCategoryImage(String imageUrl) {
        deleteImage(imageUrl, CATEGORIES_DIR);
    }

    /**
     * Delete a user profile picture.
     *
     * @param profilePictureUrl the relative URL path to the profile picture
     */
    public void deleteUserPicture(String profilePictureUrl) {
        deleteImage(profilePictureUrl, USERS_DIR);
    }

    /**
     * Generic method to delete an image.
     *
     * @param imageUrl the relative URL path to the image
     * @param uploadDir the directory where the file is stored
     */
    private void deleteImage(String imageUrl, String uploadDir) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            // Extract filename from URL path
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Path filePath = Paths.get(uploadDir, filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                LOG.debug("Deleted image: {}", filename);
            }
        } catch (IOException e) {
            LOG.error("Failed to delete image: {}", imageUrl, e);
        }
    }



    /**
     * Validate the uploaded file.
     *
     * @param file the file to validate
     * @param entityName the entity name for error messages
     */
    private void validateFile(MultipartFile file, String entityName) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestAlertException("File is required", entityName, "filerequired");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestAlertException(
                "File size exceeds maximum limit of 10 MB",
                entityName,
                "filesizeexceeded"
            );
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestAlertException(
                "Invalid file type. Only JPEG, PNG, and WebP images are allowed",
                entityName,
                "invalidfiletype"
            );
        }

        // Validate file extension
        String extension = getFileExtension(file.getOriginalFilename(), entityName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestAlertException(
                "Invalid file extension. Only jpg, jpeg, png, and webp are allowed",
                entityName,
                "invalidextension"
            );
        }
    }

    /**
     * Validate image dimensions.
     *
     * @param file the image file to validate
     * @param entityName the entity name for error messages
     * @throws IOException if image cannot be read
     */
    private void validateImageDimensions(MultipartFile file, String entityName) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());

        if (image == null) {
            throw new BadRequestAlertException(
                "Unable to read image file. File may be corrupted",
                entityName,
                "invalidimage"
            );
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width < MIN_WIDTH || height < MIN_HEIGHT) {
            throw new BadRequestAlertException(
                String.format("Image dimensions too small. Minimum size is %dx%d pixels", MIN_WIDTH, MIN_HEIGHT),
                entityName,
                "imagetoosmall"
            );
        }

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            throw new BadRequestAlertException(
                String.format("Image dimensions too large. Maximum size is %dx%d pixels", MAX_WIDTH, MAX_HEIGHT),
                entityName,
                "imagetoolarge"
            );
        }

        LOG.debug("Image dimensions validated: {}x{}", width, height);
    }

    /**
     * Get file extension from filename.
     *
     * @param filename the filename
     * @param entityName the entity name for error messages
     * @return the file extension
     */
    private String getFileExtension(String filename, String entityName) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestAlertException("Invalid filename", entityName, "invalidfilename");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Load an image file as a Resource.
     *
     * @param imageUrl the relative URL path to the image (e.g., "/media/books/cover_123.jpg")
     * @return the image file as a Resource
     * @throws IOException if the file cannot be found or read
     */
    public Resource loadImageAsResource(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new IOException("Image URL is null or empty");
        }

        try {
            // Extract filename from URL path
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

            // Determine the directory based on the URL prefix
            String directory;
            if (imageUrl.startsWith("/media/books/")) {
                directory = BOOKS_DIR;
            } else if (imageUrl.startsWith("/media/book-packs/")) {
                directory = BOOK_PACKS_DIR;
            } else if (imageUrl.startsWith("/media/authors/")) {
                directory = AUTHORS_DIR;
            } else if (imageUrl.startsWith("/media/categories/")) {
                directory = CATEGORIES_DIR;
            } else if (imageUrl.startsWith("/media/users/")) {
                directory = USERS_DIR;
            } else {
                throw new IOException("Invalid image URL: " + imageUrl);
            }

            Path filePath = Paths.get(directory).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                LOG.debug("Image loaded successfully: {}", filename);
                return resource;
            } else {
                throw new IOException("Image not found or not readable: " + imageUrl);
            }
        } catch (MalformedURLException e) {
            throw new IOException("Malformed URL: " + imageUrl, e);
        }
    }

    /**
     * Get the content type for an image file based on its extension.
     *
     * @param filename the filename
     * @return the content type (e.g., "image/jpeg", "image/png")
     */
    public String getImageContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
