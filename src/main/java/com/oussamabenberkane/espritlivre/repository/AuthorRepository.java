package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.Author;
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
     * Find author by name (case-insensitive).
     */
    Optional<Author> findByNameIgnoreCase(String name);

    /**
     * Get top 10 authors with most books.
     */
    @Query("""
        SELECT a FROM Author a
        LEFT JOIN a.books b
        GROUP BY a.id, a.name
        ORDER BY COUNT(b.id) DESC
        """)
    List<Author> findTop10AuthorsByBookCount();
}