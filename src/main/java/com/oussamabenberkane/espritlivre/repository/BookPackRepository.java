package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.BookPack;
import com.oussamabenberkane.espritlivre.service.specs.BookPackSpecifications;
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
public interface BookPackRepository extends JpaRepository<BookPack, Long>, JpaSpecificationExecutor<BookPack> {

    /**
     * Override findAll() to only return active (non-soft-deleted) book packs.
     */
    @Override
    default List<BookPack> findAll() {
        return findAll(BookPackSpecifications.activeOnly());
    }

    /**
     * Override findAll(Pageable) to only return active (non-soft-deleted) book packs.
     */
    @Override
    default Page<BookPack> findAll(Pageable pageable) {
        return findAll(BookPackSpecifications.activeOnly(), pageable);
    }

    /**
     * Override findById() to only return active (non-soft-deleted) book packs.
     */
    @Override
    default Optional<BookPack> findById(Long id) {
        return findOne(BookPackSpecifications.activeOnly().and((root, query, builder) ->
            builder.equal(root.get("id"), id)));
    }

    @Query(
        value = "select distinct bookPack from BookPack bookPack where bookPack.active = true",
        countQuery = "select count(distinct bookPack) from BookPack bookPack where bookPack.active = true"
    )
    Page<BookPack> findAllWithEagerRelationships(Pageable pageable);

    @Query("select bookPack from BookPack bookPack where bookPack.id in :ids and bookPack.active = true")
    List<BookPack> findAllByIdsWithEagerRelationships(@Param("ids") List<Long> ids);

    @Query("select bookPack from BookPack bookPack where bookPack.id = :id and bookPack.active = true")
    Optional<BookPack> findOneWithEagerRelationships(@Param("id") Long id);

    @Query(
        value = "select distinct bookPack from BookPack bookPack join bookPack.books b where b.id = :bookId and bookPack.active = true order by bookPack.createdDate desc",
        countQuery = "select count(distinct bookPack) from BookPack bookPack join bookPack.books b where b.id = :bookId and bookPack.active = true"
    )
    Page<BookPack> findByBookId(@Param("bookId") Long bookId, Pageable pageable);

    @Query("select bookPack from BookPack bookPack where bookPack.id in :ids and bookPack.active = true")
    List<BookPack> findByIdsWithEagerRelationships(@Param("ids") List<Long> ids);
}
