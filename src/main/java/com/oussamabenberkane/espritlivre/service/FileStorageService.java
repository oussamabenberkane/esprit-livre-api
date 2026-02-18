package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Service for handling file storage operations.
 */
@Service
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);

    private final ImageConversionService imageConversionService;

    @Value("${media.root-dir:src/main/resources/media}")
    private String mediaRootDir;

    public FileStorageService(ImageConversionService imageConversionService) {
        this.imageConversionService = imageConversionService;
    }

    private String booksDir;
    private String bookPacksDir;
    private String authorsDir;
    private String categoriesDir;
    private String usersDir;
    private String defaultPlaceholderPath;

    @PostConstruct
    public void init() {
        this.booksDir = mediaRootDir + "/books";
        this.bookPacksDir = mediaRootDir + "/book-packs";
        this.authorsDir = mediaRootDir + "/authors";
        this.categoriesDir = mediaRootDir + "/categories";
        this.usersDir = mediaRootDir + "/users";
        this.defaultPlaceholderPath = mediaRootDir + "/default.png";
        LOG.info("FileStorageService initialized with media root: {}", mediaRootDir);
    }

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
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
        return storeImage(file, booksDir, "cover_" + bookId, "/media/books/", "book");
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
        return storeImage(file, bookPacksDir, "pack_" + bookPackId, "/media/book-packs/", "bookPack");
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
        return storeImage(file, authorsDir, "author_" + authorId, "/media/authors/", "author");
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
        return storeImage(file, categoriesDir, "category_" + tagId, "/media/categories/", "tag");
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
        return storeImage(file, usersDir, "user_" + userId, "/media/users/", "user");
    }

    /**
     * Store the admin profile picture with fixed filename "admin.{extension}".
     *
     * @param file the uploaded file
     * @return the relative URL path to the stored file
     * @throws IOException if file storage fails
     */
    public String storeAdminPicture(MultipartFile file) throws IOException {
        LOG.debug("Request to store admin picture");
        return storeImage(file, usersDir, "admin", "/media/users/", "admin");
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
        Path uploadPath = Path.of(uploadDir);
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
        deleteImage(coverImageUrl, booksDir);
    }

    /**
     * Delete a book pack cover image.
     *
     * @param coverImageUrl the relative URL path to the cover image
     */
    public void deleteBookPackCover(String coverImageUrl) {
        deleteImage(coverImageUrl, bookPacksDir);
    }

    /**
     * Delete an author profile picture.
     *
     * @param profilePictureUrl the relative URL path to the profile picture
     */
    public void deleteAuthorPicture(String profilePictureUrl) {
        deleteImage(profilePictureUrl, authorsDir);
    }

    /**
     * Delete a category image.
     *
     * @param imageUrl the relative URL path to the category image
     */
    public void deleteCategoryImage(String imageUrl) {
        deleteImage(imageUrl, categoriesDir);
    }

    /**
     * Delete a user profile picture.
     *
     * @param profilePictureUrl the relative URL path to the profile picture
     */
    public void deleteUserPicture(String profilePictureUrl) {
        deleteImage(profilePictureUrl, usersDir);
    }

    /**
     * Delete all admin profile pictures (with any extension).
     * Since admin pictures are named admin.{extension}, this deletes all possible admin images.
     */
    public void deleteAllAdminPictures() {
        LOG.debug("Request to delete all admin pictures");

        Path usersPath = Path.of(usersDir);
        if (!Files.exists(usersPath)) {
            return;
        }

        // Delete all admin.* files
        for (String extension : ALLOWED_EXTENSIONS) {
            String filename = "admin." + extension;
            Path filePath = usersPath.resolve(filename);

            try {
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    LOG.debug("Deleted admin picture: {}", filename);
                }
            } catch (IOException e) {
                LOG.error("Failed to delete admin picture: {}", filename, e);
            }
        }
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
            Path filePath = Path.of(uploadDir, filename);

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
                "File size exceeds maximum limit of 5 MB",
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

        // if (width < MIN_WIDTH || height < MIN_HEIGHT) {
        //     throw new BadRequestAlertException(
        //         String.format("Image dimensions too small. Minimum size is %dx%d pixels", MIN_WIDTH, MIN_HEIGHT),
        //         entityName,
        //         "imagetoosmall"
        //     );
        // }

        // if (width > MAX_WIDTH || height > MAX_HEIGHT) {
        //     throw new BadRequestAlertException(
        //         String.format("Image dimensions too large. Maximum size is %dx%d pixels", MAX_WIDTH, MAX_HEIGHT),
        //         entityName,
        //         "imagetoolarge"
        //     );
        // }

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
                directory = booksDir;
            } else if (imageUrl.startsWith("/media/book-packs/")) {
                directory = bookPacksDir;
            } else if (imageUrl.startsWith("/media/authors/")) {
                directory = authorsDir;
            } else if (imageUrl.startsWith("/media/categories/")) {
                directory = categoriesDir;
            } else if (imageUrl.startsWith("/media/users/")) {
                directory = usersDir;
            } else {
                throw new IOException("Invalid image URL: " + imageUrl);
            }

            Path filePath = Path.of(directory).resolve(filename).normalize();
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

    /**
     * Load the default placeholder image as a Resource.
     *
     * @return the placeholder image file as a Resource
     * @throws IOException if the placeholder file cannot be found or read
     */
    public Resource loadPlaceholderImage() throws IOException {
        try {
            Path filePath = Path.of(defaultPlaceholderPath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                LOG.debug("Placeholder image loaded successfully");
                return resource;
            } else {
                throw new IOException("Placeholder image not found or not readable: " + defaultPlaceholderPath);
            }
        } catch (MalformedURLException e) {
            throw new IOException("Malformed placeholder path: " + defaultPlaceholderPath, e);
        }
    }
}
