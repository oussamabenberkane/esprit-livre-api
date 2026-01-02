package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.*;
import com.oussamabenberkane.espritlivre.domain.enumeration.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for scheduled cleanup of soft-deleted entities and denormalization of order data.
 *
 * This service runs daily at 2 AM to:
 * 1. Denormalize product titles for DELIVERED and CANCELLED orders
 * 2. Hard delete soft-deleted entities not referenced by active orders
 * 3. Delete associated media files
 */
@Service
@Transactional
public class CleanupService {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupService.class);

    private final FileStorageService fileStorageService;

    @PersistenceContext
    private EntityManager entityManager;

    public CleanupService(
        FileStorageService fileStorageService
    ) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Scheduled job that runs daily at 2 AM to clean up soft-deleted entities
     * and denormalize order data for DELIVERED and CANCELLED orders.
     */
    @Scheduled(cron = "0 0 2 * * *") // Every day at 2:00 AM
    public void performDailyCleanup() {
        LOG.info("Starting daily cleanup job...");

        try {
            // Step 1: Denormalize product titles for finalized orders
            int denormalizedOrders = denormalizeOrderItems();
            LOG.info("Denormalized product titles for {} orders", denormalizedOrders);

            // Step 2: Hard delete soft-deleted entities
            int deletedBooks = cleanupSoftDeletedBooks();
            LOG.info("Hard deleted {} books", deletedBooks);

            int deletedBookPacks = cleanupSoftDeletedBookPacks();
            LOG.info("Hard deleted {} book packs", deletedBookPacks);

            int deletedAuthors = cleanupSoftDeletedAuthors();
            LOG.info("Hard deleted {} authors", deletedAuthors);

            LOG.info("Daily cleanup job completed successfully. Total deletions: Books={}, BookPacks={}, Authors={}",
                deletedBooks, deletedBookPacks, deletedAuthors);

        } catch (Exception e) {
            LOG.error("Error during daily cleanup job", e);
        }
    }

    /**
     * Denormalize product titles for DELIVERED and CANCELLED orders.
     * Stores the book/pack title in the OrderItem's productTitleSnapshot field.
     *
     * @return number of orders processed
     */
    private int denormalizeOrderItems() {
        LOG.debug("Denormalizing product titles for finalized orders...");

        // Find all DELIVERED and CANCELLED orders that haven't been denormalized yet
        String jpql = """
            SELECT DISTINCT o FROM Order o
            JOIN FETCH o.orderItems oi
            WHERE o.status IN (:delivered, :cancelled)
            AND EXISTS (
                SELECT 1 FROM OrderItem item
                WHERE item.order = o
                AND item.productTitleSnapshot IS NULL
            )
            """;

        List<Order> orders = entityManager.createQuery(jpql, Order.class)
            .setParameter("delivered", OrderStatus.DELIVERED)
            .setParameter("cancelled", OrderStatus.CANCELLED)
            .getResultList();

        int processedCount = 0;
        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProductTitleSnapshot() == null) {
                    String title = extractProductTitle(item);
                    if (title != null) {
                        item.setProductTitleSnapshot(title);
                    }
                }
            }
            processedCount++;
        }

        if (processedCount > 0) {
            entityManager.flush();
        }

        return processedCount;
    }

    /**
     * Extract product title from OrderItem's Book or BookPack reference.
     */
    private String extractProductTitle(OrderItem item) {
        if (item.getBook() != null) {
            return item.getBook().getTitle();
        } else if (item.getBookPack() != null) {
            return item.getBookPack().getTitle();
        }
        return null;
    }

    /**
     * Hard delete soft-deleted Books that are not referenced by active orders.
     * Also deletes associated media files.
     *
     * @return number of books deleted
     */
    private int cleanupSoftDeletedBooks() {
        LOG.debug("Cleaning up soft-deleted books...");

        // Find soft-deleted books NOT referenced by active orders
        String jpql = """
            SELECT b FROM Book b
            WHERE b.active = false
            AND b.deletedAt IS NOT NULL
            AND NOT EXISTS (
                SELECT 1 FROM OrderItem oi
                JOIN oi.order o
                WHERE oi.book = b
                AND o.status NOT IN (:delivered, :cancelled)
            )
            """;

        List<Book> booksToDelete = entityManager.createQuery(jpql, Book.class)
            .setParameter("delivered", OrderStatus.DELIVERED)
            .setParameter("cancelled", OrderStatus.CANCELLED)
            .getResultList();

        int deletedCount = 0;
        for (Book book : booksToDelete) {
            // Nullify FK in OrderItems before deletion
            nullifyBookReferences(book.getId());

            // Delete cover image
            if (book.getCoverImageUrl() != null) {
                try {
                    fileStorageService.deleteBookCover(book.getCoverImageUrl());
                } catch (Exception e) {
                    LOG.warn("Failed to delete book cover for book {}: {}", book.getId(), e.getMessage());
                }
            }

            // Hard delete the book
            entityManager.remove(book);
            deletedCount++;
        }

        if (deletedCount > 0) {
            entityManager.flush();
        }

        return deletedCount;
    }

    /**
     * Nullify book FK references in OrderItems before hard deleting the book.
     */
    private void nullifyBookReferences(Long bookId) {
        String updateJpql = "UPDATE OrderItem oi SET oi.book = NULL WHERE oi.book.id = :bookId";
        entityManager.createQuery(updateJpql)
            .setParameter("bookId", bookId)
            .executeUpdate();
    }

    /**
     * Hard delete soft-deleted BookPacks that are not referenced by active orders.
     * Also deletes associated media files.
     *
     * @return number of book packs deleted
     */
    private int cleanupSoftDeletedBookPacks() {
        LOG.debug("Cleaning up soft-deleted book packs...");

        String jpql = """
            SELECT bp FROM BookPack bp
            WHERE bp.active = false
            AND bp.deletedAt IS NOT NULL
            AND NOT EXISTS (
                SELECT 1 FROM OrderItem oi
                JOIN oi.order o
                WHERE oi.bookPack = bp
                AND o.status NOT IN (:delivered, :cancelled)
            )
            """;

        List<BookPack> packsToDelete = entityManager.createQuery(jpql, BookPack.class)
            .setParameter("delivered", OrderStatus.DELIVERED)
            .setParameter("cancelled", OrderStatus.CANCELLED)
            .getResultList();

        int deletedCount = 0;
        for (BookPack pack : packsToDelete) {
            // Nullify FK in OrderItems before deletion
            nullifyBookPackReferences(pack.getId());

            // Delete cover image
            if (pack.getCoverUrl() != null) {
                try {
                    fileStorageService.deleteBookPackCover(pack.getCoverUrl());
                } catch (Exception e) {
                    LOG.warn("Failed to delete book pack cover for pack {}: {}", pack.getId(), e.getMessage());
                }
            }

            // Hard delete the pack
            entityManager.remove(pack);
            deletedCount++;
        }

        if (deletedCount > 0) {
            entityManager.flush();
        }

        return deletedCount;
    }

    /**
     * Nullify bookPack FK references in OrderItems before hard deleting the pack.
     */
    private void nullifyBookPackReferences(Long bookPackId) {
        String updateJpql = "UPDATE OrderItem oi SET oi.bookPack = NULL WHERE oi.bookPack.id = :bookPackId";
        entityManager.createQuery(updateJpql)
            .setParameter("bookPackId", bookPackId)
            .executeUpdate();
    }

    /**
     * Hard delete soft-deleted Authors that are not referenced by active Books.
     * Also deletes associated media files.
     *
     * @return number of authors deleted
     */
    private int cleanupSoftDeletedAuthors() {
        LOG.debug("Cleaning up soft-deleted authors...");

        String jpql = """
            SELECT a FROM Author a
            WHERE a.active = false
            AND a.deletedAt IS NOT NULL
            AND NOT EXISTS (
                SELECT 1 FROM Book b
                WHERE b.author = a
                AND b.active = true
            )
            """;

        List<Author> authorsToDelete = entityManager.createQuery(jpql, Author.class)
            .getResultList();

        int deletedCount = 0;
        for (Author author : authorsToDelete) {
            // Nullify FK in Books before deletion
            nullifyAuthorReferences(author.getId());

            // Delete profile picture
            if (author.getProfilePictureUrl() != null) {
                try {
                    fileStorageService.deleteAuthorPicture(author.getProfilePictureUrl());
                } catch (Exception e) {
                    LOG.warn("Failed to delete author picture for author {}: {}", author.getId(), e.getMessage());
                }
            }

            // Hard delete the author
            entityManager.remove(author);
            deletedCount++;
        }

        if (deletedCount > 0) {
            entityManager.flush();
        }

        return deletedCount;
    }

    /**
     * Nullify author FK references in Books before hard deleting the author.
     */
    private void nullifyAuthorReferences(Long authorId) {
        String updateJpql = "UPDATE Book b SET b.author = NULL WHERE b.author.id = :authorId";
        entityManager.createQuery(updateJpql)
            .setParameter("authorId", authorId)
            .executeUpdate();
    }
}
