package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.service.dto.BookDTO;
import com.oussamabenberkane.espritlivre.service.mapper.BookMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.oussamabenberkane.espritlivre.service.specs.BookSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service Implementation for managing {@link com.oussamabenberkane.espritlivre.domain.Book}.
 */
@Service
@Transactional
public class BookService {

    private static final Logger LOG = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;

    @PersistenceContext
    private EntityManager em;

    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    /**
     * Save a book.
     *
     * @param bookDTO the entity to save.
     * @return the persisted entity.
     */
    public BookDTO save(BookDTO bookDTO) {
        LOG.debug("Request to save Book : {}", bookDTO);
        Book book = bookMapper.toEntity(bookDTO);
        book = bookRepository.save(book);
        return bookMapper.toDto(book);
    }

    /**
     * Update a book.
     *
     * @param bookDTO the entity to save.
     * @return the persisted entity.
     */
    public BookDTO update(BookDTO bookDTO) {
        LOG.debug("Request to update Book : {}", bookDTO);
        Book book = bookMapper.toEntity(bookDTO);
        book = bookRepository.save(book);
        return bookMapper.toDto(book);
    }

    @Transactional(readOnly = true)
    public Page<BookDTO> findAll(
        Pageable pageable,
        String author,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Long categoryId,
        Long mainDisplayId
    ) {
        LOG.debug("Request to get all Books with filters - author: {}, priceRange: [{}, {}], categoryId: {}, mainDisplayId: {}",
            author, minPrice, maxPrice, categoryId, mainDisplayId);

        Specification<Book> spec = Specification.where(null);

        // Apply individual filters only if parameters are provided
        if (StringUtils.hasText(author)) {
            spec = spec.and(BookSpecifications.hasAuthor(author));
        }

        if (minPrice != null || maxPrice != null) {
            spec = spec.and(BookSpecifications.hasPriceBetween(minPrice, maxPrice));
        }

        // Tag filtering - books that have EITHER category OR mainDisplay tag
        if (categoryId != null || mainDisplayId != null) {
            Specification<Book> tagSpec = buildTagSpecification(categoryId, mainDisplayId);
            if (tagSpec != null) {
                spec = spec.and(tagSpec);
            }
        }

        Page<Book> books = bookRepository.findAll(spec, pageable);
        return books.map(bookMapper::toDto);
    }

    private Specification<Book> buildTagSpecification(Long categoryId, Long mainDisplayId) {
        Specification<Book> tagSpec = null;

        if (categoryId != null) {
            tagSpec = BookSpecifications.hasCategory(categoryId);
        }

        if (mainDisplayId != null) {
            Specification<Book> mainDisplaySpec = BookSpecifications.hasMainDisplay(mainDisplayId);
            tagSpec = tagSpec != null ? tagSpec.or(mainDisplaySpec) : mainDisplaySpec;
        }

        return tagSpec;
    }

    /**
     * Get one book by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<BookDTO> findOne(Long id) {
        LOG.debug("Request to get Book : {}", id);
        return bookRepository.findById(id).map(bookMapper::toDto);
    }

    /**
     * Delete the book by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Book : {}", id);
        bookRepository.deleteById(id);
    }
}
