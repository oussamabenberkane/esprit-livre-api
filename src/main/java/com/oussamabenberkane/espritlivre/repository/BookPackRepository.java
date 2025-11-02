package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.BookPack;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the BookPack entity.
 */
@Repository
public interface BookPackRepository extends JpaRepository<BookPack, Long> {
    @Query(
        value = "select distinct bookPack from BookPack bookPack left join fetch bookPack.books",
        countQuery = "select count(distinct bookPack) from BookPack bookPack"
    )
    Page<BookPack> findAllWithEagerRelationships(Pageable pageable);

    @Query("select bookPack from BookPack bookPack left join fetch bookPack.books where bookPack.id = :id")
    Optional<BookPack> findOneWithEagerRelationships(@Param("id") Long id);

    @Query(
        value = "select distinct bookPack from BookPack bookPack join bookPack.books b where b.id = :bookId order by bookPack.createdDate desc",
        countQuery = "select count(distinct bookPack) from BookPack bookPack join bookPack.books b where b.id = :bookId"
    )
    Page<BookPack> findByBookId(@Param("bookId") Long bookId, Pageable pageable);

    @Query("select bookPack from BookPack bookPack left join fetch bookPack.books where bookPack.id in :ids")
    List<BookPack> findByIdsWithEagerRelationships(@Param("ids") List<Long> ids);
}
