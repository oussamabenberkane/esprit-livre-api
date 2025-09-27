package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.Book;
import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.service.dto.BookDTO;
import com.oussamabenberkane.espritlivre.service.dto.BookSuggestionDTO;
import com.oussamabenberkane.espritlivre.service.mapper.BookMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        String search,
        String author,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Long categoryId,
        Long mainDisplayId
    ) {
        LOG.debug("Request to get all Books with filters - search: {}, author: {}, priceRange: [{}, {}], categoryId: {}, mainDisplayId: {}",
            search, author, minPrice, maxPrice, categoryId, mainDisplayId);

        Specification<Book> spec = Specification.where(null);

        // Apply search filter if provided
        if (StringUtils.hasText(search)) {
            spec = spec.and(BookSpecifications.searchByText(search));
        }

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

    /**
     * Get suggestions for search terms.
     *
     * @param searchTerm the search term to get suggestions for.
     * @return the list of suggestions.
     */
    @Transactional(readOnly = true)
    public List<BookSuggestionDTO> getSuggestions(String searchTerm) {
        LOG.debug("Request to get suggestions for search term: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<BookSuggestionDTO> suggestions = new ArrayList<>();
        String searchPattern = "%" + searchTerm.toLowerCase().trim() + "%";

        // Get book title suggestions
        String titleQuery = """
            SELECT DISTINCT b.title
            FROM Book b
            WHERE LOWER(b.title) LIKE :searchPattern
            ORDER BY b.title
            """;

        List<String> titles = em.createQuery(titleQuery, String.class)
            .setParameter("searchPattern", searchPattern)
            .setMaxResults(5)
            .getResultList();

        titles.forEach(title -> suggestions.add(
            new BookSuggestionDTO(title, BookSuggestionDTO.SuggestionType.BOOK_TITLE)
        ));

        // Get author suggestions
        String authorQuery = """
            SELECT DISTINCT a.name
            FROM Author a
            WHERE LOWER(a.name) LIKE :searchPattern
            ORDER BY a.name
            """;

        List<String> authors = em.createQuery(authorQuery, String.class)
            .setParameter("searchPattern", searchPattern)
            .setMaxResults(5)
            .getResultList();

        authors.forEach(author -> suggestions.add(
            new BookSuggestionDTO(author, BookSuggestionDTO.SuggestionType.AUTHOR)
        ));

        // Get category suggestions (from tags)
        String categoryQuery = """
            SELECT DISTINCT t.nameEn
            FROM Tag t
            WHERE (LOWER(t.nameEn) LIKE :searchPattern OR LOWER(t.nameFr) LIKE :searchPattern)
            AND t.type = com.oussamabenberkane.espritlivre.domain.enumeration.TagType.CATEGORY
            ORDER BY t.nameEn
            """;

        List<String> categories = em.createQuery(categoryQuery, String.class)
            .setParameter("searchPattern", searchPattern)
            .setMaxResults(5)
            .getResultList();

        categories.forEach(category -> suggestions.add(
            new BookSuggestionDTO(category, BookSuggestionDTO.SuggestionType.CATEGORY)
        ));

        return suggestions.stream()
            .distinct()
            .limit(15)
            .collect(Collectors.toList());
    }
}
