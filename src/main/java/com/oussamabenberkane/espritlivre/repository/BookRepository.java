package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Book;
import jakarta.persistence.LockModeType;
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
    @Query("select b from Book b join Like l on l.book.id = b.id left join fetch b.author where l.user.login = ?#{authentication.name}")
    Page<Book> findLikedBooksByCurrentUser(Specification<Book> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "tags"})
    Optional<Book> findWithAuthorAndTagsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity - :quantity WHERE b.id = :bookId AND b.stockQuantity >= :quantity")
    int decrementStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Book b SET b.stockQuantity = b.stockQuantity + :quantity WHERE b.id = :bookId")
    int incrementStock(@Param("bookId") Long bookId, @Param("quantity") Integer quantity);
}
