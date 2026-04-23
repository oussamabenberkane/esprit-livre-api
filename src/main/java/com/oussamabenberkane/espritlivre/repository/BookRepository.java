package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.service.specs.BookSpecifications;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    /**
     * Override findAll() to only return active (non-soft-deleted) books.
     */
    @Override
    default List<Book> findAll() {
        return findAll(BookSpecifications.activeOnly());
    }

    /**
     * Override findAll(Pageable) to only return active (non-soft-deleted) books.
     */
    @Override
    default Page<Book> findAll(Pageable pageable) {
        return findAll(BookSpecifications.activeOnly(), pageable);
    }

    /**
     * Override findById() to only return active (non-soft-deleted) books.
     */
    @Override
    default Optional<Book> findById(Long id) {
        return findOne(BookSpecifications.activeOnly().and((root, query, builder) ->
            builder.equal(root.get("id"), id)));
    }

    @Query("select b from Book b join Like l on l.book.id = b.id left join fetch b.author where l.user.login = ?#{authentication.name} and b.active = true")
    Page<Book> findLikedBooksByCurrentUser(Specification<Book> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "tags"})
    Optional<Book> findWithAuthorAndTagsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id and b.active = true")
    Optional<Book> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity - :quantity WHERE b.id = :bookId AND b.active = true AND b.stockQuantity >= :quantity")
    int decrementStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity WHERE b.id = :bookId AND b.active = true")
    int incrementStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity);

    /**
     * Find similar books by shared tags or same author, excluding the source book.
     * Returns [bookId, sharedTagCount, sameAuthor (1/0), sameLanguage (1/0)].
     */
    @Query(value = """
        SELECT b.id AS book_id,
               COALESCE(tag_overlap.shared_tags, 0) AS shared_tags,
               CASE WHEN b.author_id = :authorId THEN 1 ELSE 0 END AS same_author,
               CASE WHEN b.language = :language THEN 1 ELSE 0 END AS same_language
        FROM book b
        LEFT JOIN (
            SELECT tb2.book_id, COUNT(*) AS shared_tags
            FROM rel_tag__book tb1
            JOIN rel_tag__book tb2 ON tb1.tag_id = tb2.tag_id AND tb2.book_id != :bookId
            WHERE tb1.book_id = :bookId
            GROUP BY tb2.book_id
        ) tag_overlap ON tag_overlap.book_id = b.id
        WHERE b.id != :bookId
          AND b.active = true
          AND (tag_overlap.shared_tags > 0 OR b.author_id = :authorId)
        """, nativeQuery = true)
    List<Object[]> findSimilarBookCandidates(
        @Param("bookId") Long bookId,
        @Param("authorId") Long authorId,
        @Param("language") String language
    );

    /**
     * Find books that co-occur with the given book in book packs.
     * Returns [bookId, cooccurrenceCount].
     */
    @Query(value = """
        SELECT r2.books_id AS book_id, COUNT(DISTINCT r1.book_pack_id) AS cooccurrence
        FROM rel_book_pack__books r1
        JOIN rel_book_pack__books r2 ON r2.book_pack_id = r1.book_pack_id AND r2.books_id != :bookId
        JOIN book_pack bp ON bp.id = r1.book_pack_id AND bp.active = true
        JOIN book b ON b.id = r2.books_id AND b.active = true
        WHERE r1.books_id = :bookId
        GROUP BY r2.books_id
        """, nativeQuery = true)
    List<Object[]> findCoOccurringBooks(@Param("bookId") Long bookId);

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.tags WHERE b.id IN :ids AND b.active = true")
    List<Book> findAllByIdsWithEagerRelationships(@Param("ids") List<Long> ids);

    @Query(
        value = "SELECT b.* FROM book b " +
                "INNER JOIN rel_tag__book rtb ON b.id = rtb.book_id " +
                "WHERE rtb.tag_id = :tagId AND b.active = true " +
                "ORDER BY rtb.book_order ASC NULLS LAST",
        countQuery = "SELECT COUNT(b.id) FROM book b " +
                     "INNER JOIN rel_tag__book rtb ON b.id = rtb.book_id " +
                     "WHERE rtb.tag_id = :tagId AND b.active = true",
        nativeQuery = true
    )
    Page<Book> findByMainDisplayIdOrdered(@Param("tagId") Long tagId, Pageable pageable);
}
