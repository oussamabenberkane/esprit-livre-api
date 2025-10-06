package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.service.BookService;
import com.oussamabenberkane.espritlivre.service.ValidationService;
import com.oussamabenberkane.espritlivre.service.dto.BookDTO;
import com.oussamabenberkane.espritlivre.service.dto.BookSuggestionDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.oussamabenberkane.espritlivre.domain.Book}.
 */
@RestController
@RequestMapping("/api/books")
public class BookResource {

    private static final Logger LOG = LoggerFactory.getLogger(BookResource.class);

    private static final String ENTITY_NAME = "book";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final BookService bookService;

    private final BookRepository bookRepository;

    private final ValidationService validationService;

    public BookResource(BookService bookService, BookRepository bookRepository, ValidationService validationService) {
        this.bookService = bookService;
        this.bookRepository = bookRepository;
        this.validationService = validationService;
    }

    /**
     * {@code POST  /books} : Create a new book with cover image.
     *
     * @param bookDTO the bookDTO to create (as JSON part).
     * @param coverImage the book cover image file.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new bookDTO, or with status {@code 400 (Bad Request)} if validation fails.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping(value = "", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookDTO> createBook(
        @RequestPart("book") @Valid BookDTO bookDTO,
        @RequestPart("coverImage") @NotNull org.springframework.web.multipart.MultipartFile coverImage
    ) throws URISyntaxException {
        LOG.debug("REST request to create Book : {}", bookDTO);

        if (bookDTO.getId() != null) {
            throw new BadRequestAlertException("A new book cannot already have an ID", ENTITY_NAME, "idexists");
        }

        BookDTO result = bookService.createBookWithCover(bookDTO, coverImage);

        return ResponseEntity.created(new URI("/api/books/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /books/:id} : Update an existing book with optional cover image.
     *
     * @param id the id of the bookDTO to update.
     * @param bookDTO the bookDTO to update (as JSON part).
     * @param coverImage the book cover image file (optional).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated bookDTO,
     * or with status {@code 400 (Bad Request)} if the bookDTO is not valid,
     * or with status {@code 404 (Not Found)} if the book does not exist.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookDTO> updateBook(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestPart("book") @Valid BookDTO bookDTO,
        @RequestPart(value = "coverImage", required = false) org.springframework.web.multipart.MultipartFile coverImage
    ) throws URISyntaxException {
        LOG.debug("REST request to update Book : {}, {}", id, bookDTO);

        if (bookDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        if (!Objects.equals(id, bookDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!bookRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        BookDTO result = bookService.updateBookWithCover(bookDTO, coverImage);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, bookDTO.getId().toString()))
            .body(result);
    }

    @GetMapping("")
    public ResponseEntity<Page<BookDTO>> getAllBooks(
        @ParameterObject Pageable pageable,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) @DecimalMin("0") BigDecimal minPrice,
        @RequestParam(required = false) @DecimalMin("0") BigDecimal maxPrice,
        @RequestParam(required = false) @Min(1) Long categoryId,
        @RequestParam(required = false) @Min(1) Long mainDisplayId
    ) {
        validationService.validatePriceRange(minPrice, maxPrice, ENTITY_NAME);
        Page<BookDTO> page = bookService.findAll(pageable, search, author, minPrice, maxPrice, categoryId, mainDisplayId);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page);
    }

    /**
     * {@code GET  /books/liked} : get all books liked by current user.
     *
     * @param pageable the pagination information.
     * @param search the search term.
     * @param author the author name filter.
     * @param minPrice the minimum price filter.
     * @param maxPrice the maximum price filter.
     * @param categoryId the category tag id filter.
     * @param mainDisplayId the main display tag id filter.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of liked books.
     */
    @GetMapping("/liked")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<Page<BookDTO>> getLikedBooks(
        @ParameterObject Pageable pageable,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) @DecimalMin("0") BigDecimal minPrice,
        @RequestParam(required = false) @DecimalMin("0") BigDecimal maxPrice,
        @RequestParam(required = false) @Min(1) Long categoryId,
        @RequestParam(required = false) @Min(1) Long mainDisplayId
    ) {
        LOG.debug("REST request to get liked books");
        validationService.validatePriceRange(minPrice, maxPrice, ENTITY_NAME);
        Page<BookDTO> page = bookService.findLikedBooksByCurrentUser(pageable, search, author, minPrice, maxPrice, categoryId, mainDisplayId);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page);
    }

    /**
     * {@code GET  /books/suggestions} : get search suggestions.
     *
     * @param q the search term to get suggestions for.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of suggestions.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<BookSuggestionDTO>> getSuggestions(@RequestParam("q") String q) {
        LOG.debug("REST request to get suggestions for term : {}", q);
        List<BookSuggestionDTO> suggestions = bookService.getSuggestions(q);
        return ResponseEntity.ok().body(suggestions);
    }

    /**
     * {@code GET  /books/:id} : get the "id" book.
     *
     * @param id the id of the bookDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the bookDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBook(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Book : {}", id);
        Optional<BookDTO> bookDTO = bookService.findOne(id);
        return ResponseUtil.wrapOrNotFound(bookDTO);
    }

    /**
     * {@code DELETE  /books/:id} : soft delete the "id" book (sets active = false).
     *
     * @param id the id of the bookDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteBook(@PathVariable("id") Long id) {
        LOG.debug("REST request to soft delete Book : {}", id);
        bookService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code DELETE  /books/:id/forever} : hard delete the "id" book (permanently removes from database).
     * WARNING: This cannot be undone and will affect order history.
     *
     * @param id the id of the bookDTO to permanently delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}/forever")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteBookForever(@PathVariable("id") Long id) {
        LOG.debug("REST request to permanently delete Book : {}", id);
        bookService.deleteForever(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
