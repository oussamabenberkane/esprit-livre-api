package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Author;
import com.oussamabenberkane.espritlivre.service.specs.AuthorSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Author entity.
 */
@Repository
public interface AuthorRepository extends JpaRepository<Author, Long>, JpaSpecificationExecutor<Author> {

    /**
     * Override findAll() to only return active (non-soft-deleted) authors.
     */
    @Override
    default List<Author> findAll() {
        return findAll(AuthorSpecifications.activeOnly());
    }

    /**
     * Override findAll(Pageable) to only return active (non-soft-deleted) authors.
     */
    @Override
    default Page<Author> findAll(Pageable pageable) {
        return findAll(AuthorSpecifications.activeOnly(), pageable);
    }

    /**
     * Override findById() to only return active (non-soft-deleted) authors.
     */
    @Override
    default Optional<Author> findById(Long id) {
        return findOne(AuthorSpecifications.activeOnly().and((root, query, builder) ->
            builder.equal(root.get("id"), id)));
    }

    /**
     * Find author by name (case-insensitive).
     */
    @Query("SELECT a FROM Author a WHERE LOWER(a.name) = LOWER(:name) AND a.active = true")
    Optional<Author> findByNameIgnoreCase(String name);

    /**
     * Get top 10 authors with most books.
     */
    @Query("""
        SELECT a FROM Author a
        LEFT JOIN a.books b
        WHERE a.active = true
        GROUP BY a.id, a.name
        ORDER BY COUNT(b.id) DESC
        """)
    List<Author> findTop10AuthorsByBookCount();
}