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
}
