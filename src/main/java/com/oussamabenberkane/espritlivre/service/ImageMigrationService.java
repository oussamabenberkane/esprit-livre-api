package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Author;
import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.domain.Tag;
import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.repository.AuthorRepository;
import com.oussamabenberkane.espritlivre.repository.BookPackRepository;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.repository.TagRepository;
import com.oussamabenberkane.espritlivre.repository.UserRepository;
import com.oussamabenberkane.espritlivre.service.dto.MigrationStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for migrating existing images to WebP format.
 */
@Service
public class ImageMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(ImageMigrationService.class);

    private final ImageConversionService imageConversionService;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookPackRepository bookPackRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Value("${media.root-dir:src/main/resources/media}")
    private String mediaRootDir;

    private final AtomicBoolean migrationInProgress = new AtomicBoolean(false);
    private final AtomicInteger totalImages = new AtomicInteger(0);
    private final AtomicInteger processedImages = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final List<String> errors = new ArrayList<>();

    public ImageMigrationService(
        ImageConversionService imageConversionService,
        BookRepository bookRepository,
        AuthorRepository authorRepository,
        BookPackRepository bookPackRepository,
        TagRepository tagRepository,
        UserRepository userRepository
    ) {
        this.imageConversionService = imageConversionService;
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.bookPackRepository = bookPackRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    /**
     * Start the image migration process asynchronously.
     *
     * @return true if migration started, false if already in progress
     */
    @Async
    public void startMigration() {
        if (!migrationInProgress.compareAndSet(false, true)) {
            LOG.warn("Migration already in progress");
            return;
        }

        try {
            resetCounters();
            LOG.info("Starting image migration to WebP format");

            // Count total images first
            int total = countNonWebPImages();
            totalImages.set(total);
            LOG.info("Found {} non-WebP images to migrate", total);

            if (total == 0) {
                LOG.info("No images to migrate");
                return;
            }

            // Migrate each directory
            migrateDirectory("books", "cover_", this::updateBookUrl);
            migrateDirectory("authors", "author_", this::updateAuthorUrl);
            migrateDirectory("book-packs", "pack_", this::updateBookPackUrl);
            migrateDirectory("categories", "category_", this::updateTagUrl);
            migrateDirectory("users", null, this::updateUserUrl);

            // Migrate default.png if it exists
            migrateDefaultImage();

            LOG.info("Migration completed. Success: {}, Errors: {}", successCount.get(), errorCount.get());
        } catch (Exception e) {
            LOG.error("Migration failed with exception", e);
            addError("Migration failed: " + e.getMessage());
        } finally {
            migrationInProgress.set(false);
        }
    }

    /**
     * Get the current migration status.
     *
     * @return MigrationStatusDTO with current progress
     */
    public MigrationStatusDTO getStatus() {
        synchronized (errors) {
            return new MigrationStatusDTO(
                migrationInProgress.get(),
                totalImages.get(),
                processedImages.get(),
                successCount.get(),
                errorCount.get(),
                new ArrayList<>(errors)
            );
        }
    }

    /**
     * Check if migration is currently in progress.
     *
     * @return true if migration is running
     */
    public boolean isMigrationInProgress() {
        return migrationInProgress.get();
    }

    private void resetCounters() {
        totalImages.set(0);
        processedImages.set(0);
        successCount.set(0);
        errorCount.set(0);
        synchronized (errors) {
            errors.clear();
        }
    }

    private void addError(String error) {
        synchronized (errors) {
            errors.add(error);
        }
        errorCount.incrementAndGet();
    }

    private int countNonWebPImages() {
        int count = 0;
        String[] directories = {"books", "authors", "book-packs", "categories", "users"};

        for (String dir : directories) {
            Path dirPath = Path.of(mediaRootDir, dir);
            if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                    for (Path file : stream) {
                        if (isNonWebPImage(file)) {
                            count++;
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Error counting files in {}", dir, e);
                }
            }
        }

        // Check default.png
        Path defaultImage = Path.of(mediaRootDir, "default.png");
        if (Files.exists(defaultImage)) {
            count++;
        }

        return count;
    }

    private boolean isNonWebPImage(Path file) {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        String filename = file.getFileName().toString().toLowerCase();
        return (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
            && !filename.endsWith(".webp");
    }

    private void migrateDirectory(String dirName, String filenamePrefix, UrlUpdater urlUpdater) {
        Path dirPath = Path.of(mediaRootDir, dirName);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            LOG.debug("Directory does not exist: {}", dirPath);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path file : stream) {
                if (isNonWebPImage(file)) {
                    migrateFile(file, dirName, filenamePrefix, urlUpdater);
                }
            }
        } catch (IOException e) {
            LOG.error("Error reading directory {}", dirName, e);
            addError("Error reading directory " + dirName + ": " + e.getMessage());
        }
    }

    private void migrateFile(Path file, String dirName, String filenamePrefix, UrlUpdater urlUpdater) {
        String originalFilename = file.getFileName().toString();
        String baseFilename = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        String webpFilename = baseFilename + ".webp";
        Path webpPath = file.getParent().resolve(webpFilename);

        try {
            // Convert to WebP
            try (InputStream is = Files.newInputStream(file)) {
                byte[] webpData = imageConversionService.convertToWebP(is);
                Files.write(webpPath, webpData);
            }

            // Update database URL
            String oldUrl = "/media/" + dirName + "/" + originalFilename;
            String newUrl = "/media/" + dirName + "/" + webpFilename;

            try {
                urlUpdater.updateUrl(oldUrl, newUrl, baseFilename);
            } catch (Exception e) {
                LOG.warn("Could not update database URL for {}: {}", originalFilename, e.getMessage());
            }

            // Delete original file
            Files.delete(file);

            LOG.debug("Migrated {} to WebP", originalFilename);
            successCount.incrementAndGet();
        } catch (IOException e) {
            LOG.error("Failed to migrate {}: {}", originalFilename, e.getMessage());
            addError("Failed to migrate " + originalFilename + ": " + e.getMessage());
        } finally {
            processedImages.incrementAndGet();
        }
    }

    private void migrateDefaultImage() {
        Path defaultPng = Path.of(mediaRootDir, "default.png");
        Path defaultWebp = Path.of(mediaRootDir, "default.webp");

        if (!Files.exists(defaultPng)) {
            LOG.debug("default.png does not exist, skipping");
            return;
        }

        try {
            try (InputStream is = Files.newInputStream(defaultPng)) {
                byte[] webpData = imageConversionService.convertToWebP(is);
                Files.write(defaultWebp, webpData);
            }

            Files.delete(defaultPng);

            LOG.info("Migrated default.png to default.webp");
            successCount.incrementAndGet();
        } catch (IOException e) {
            LOG.error("Failed to migrate default.png: {}", e.getMessage());
            addError("Failed to migrate default.png: " + e.getMessage());
        } finally {
            processedImages.incrementAndGet();
        }
    }

    @Transactional
    public void updateBookUrl(String oldUrl, String newUrl, String baseFilename) {
        // Extract book ID from filename like "cover_123"
        if (baseFilename.startsWith("cover_")) {
            try {
                Long id = Long.parseLong(baseFilename.substring(6));
                bookRepository.findById(id).ifPresent(book -> {
                    if (oldUrl.equals(book.getCoverImageUrl())) {
                        book.setCoverImageUrl(newUrl);
                        bookRepository.save(book);
                        LOG.debug("Updated book {} coverImageUrl to {}", id, newUrl);
                    }
                });
            } catch (NumberFormatException e) {
                LOG.warn("Could not parse book ID from filename: {}", baseFilename);
            }
        }
    }

    @Transactional
    public void updateAuthorUrl(String oldUrl, String newUrl, String baseFilename) {
        if (baseFilename.startsWith("author_")) {
            try {
                Long id = Long.parseLong(baseFilename.substring(7));
                authorRepository.findById(id).ifPresent(author -> {
                    if (oldUrl.equals(author.getProfilePictureUrl())) {
                        author.setProfilePictureUrl(newUrl);
                        authorRepository.save(author);
                        LOG.debug("Updated author {} profilePictureUrl to {}", id, newUrl);
                    }
                });
            } catch (NumberFormatException e) {
                LOG.warn("Could not parse author ID from filename: {}", baseFilename);
            }
        }
    }

    @Transactional
    public void updateBookPackUrl(String oldUrl, String newUrl, String baseFilename) {
        if (baseFilename.startsWith("pack_")) {
            try {
                Long id = Long.parseLong(baseFilename.substring(5));
                bookPackRepository.findById(id).ifPresent(bookPack -> {
                    if (oldUrl.equals(bookPack.getCoverUrl())) {
                        bookPack.setCoverUrl(newUrl);
                        bookPackRepository.save(bookPack);
                        LOG.debug("Updated bookPack {} coverUrl to {}", id, newUrl);
                    }
                });
            } catch (NumberFormatException e) {
                LOG.warn("Could not parse bookPack ID from filename: {}", baseFilename);
            }
        }
    }

    @Transactional
    public void updateTagUrl(String oldUrl, String newUrl, String baseFilename) {
        if (baseFilename.startsWith("category_")) {
            try {
                Long id = Long.parseLong(baseFilename.substring(9));
                tagRepository.findById(id).ifPresent(tag -> {
                    if (oldUrl.equals(tag.getImageUrl())) {
                        tag.setImageUrl(newUrl);
                        tagRepository.save(tag);
                        LOG.debug("Updated tag {} imageUrl to {}", id, newUrl);
                    }
                });
            } catch (NumberFormatException e) {
                LOG.warn("Could not parse tag ID from filename: {}", baseFilename);
            }
        }
    }

    @Transactional
    public void updateUserUrl(String oldUrl, String newUrl, String baseFilename) {
        if (baseFilename.startsWith("user_")) {
            String userId = baseFilename.substring(5);
            userRepository.findById(userId).ifPresent(user -> {
                if (oldUrl.equals(user.getImageUrl())) {
                    user.setImageUrl(newUrl);
                    userRepository.save(user);
                    LOG.debug("Updated user {} imageUrl to {}", userId, newUrl);
                }
            });
        } else if (baseFilename.equals("admin")) {
            // Admin picture uses a special filename
            userRepository.findAll().stream()
                .filter(user -> oldUrl.equals(user.getImageUrl()))
                .forEach(user -> {
                    user.setImageUrl(newUrl);
                    userRepository.save(user);
                    LOG.debug("Updated admin user imageUrl to {}", newUrl);
                });
        }
    }

    @FunctionalInterface
    private interface UrlUpdater {
        void updateUrl(String oldUrl, String newUrl, String baseFilename);
    }
}
