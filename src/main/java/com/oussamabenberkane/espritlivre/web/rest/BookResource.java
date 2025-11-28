package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.repository.BookRepository;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.service.BookPackService;
import com.oussamabenberkane.espritlivre.service.BookService;
import com.oussamabenberkane.espritlivre.service.FileStorageService;
import com.oussamabenberkane.espritlivre.service.ValidationService;
import com.oussamabenberkane.espritlivre.service.dto.BookDTO;
import com.oussamabenberkane.espritlivre.service.dto.BookPackDTO;
import com.oussamabenberkane.espritlivre.service.dto.BookSuggestionDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
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
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    private final FileStorageService fileStorageService;

    private final BookPackService bookPackService;

    public BookResource(BookService bookService, BookRepository bookRepository, ValidationService validationService, FileStorageService fileStorageService, BookPackService bookPackService) {
        this.bookService = bookService;
        this.bookRepository = bookRepository;
        this.validationService = validationService;
        this.fileStorageService = fileStorageService;
        this.bookPackService = bookPackService;
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
        @RequestParam(required = false) List<Long> author,
        @RequestParam(required = false) @DecimalMin("0") BigDecimal minPrice,
        @RequestParam(required = false) @DecimalMin("0") BigDecimal maxPrice,
        @RequestParam(required = false) @Min(1) Long categoryId,
        @RequestParam(required = false) @Min(1) Long mainDisplayId,
        @RequestParam(required = false) List<String> language
    ) {
        validationService.validatePriceRange(minPrice, maxPrice, ENTITY_NAME);
        Page<BookDTO> page = bookService.findAll(pageable, search, author, minPrice, maxPrice, categoryId, mainDisplayId, language);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page);
    }

    /**
     * {@code GET  /books/liked} : get all books liked by current user.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of liked books.
     */
    @GetMapping("/liked")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<Page<BookDTO>> getLikedBooks(
        @ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get liked books");
        Page<BookDTO> page = bookService.findLikedBooksByCurrentUser(pageable);
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
     * {@code GET  /books/:id/cover} : get the cover image for the "id" book.
     * This endpoint is publicly accessible without authentication.
     *
     * @param id the id of the book.
     * @param placeholder optional parameter to return a placeholder image if the cover is not found.
     * @return the {@link ResponseEntity} with the image file, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> getBookCover(
        @PathVariable("id") Long id,
        @RequestParam(value = "placeholder", required = false, defaultValue = "false") boolean placeholder
    ) {
        LOG.debug("REST request to get Book cover : {}", id);

        Optional<BookDTO> bookDTO = bookService.findOne(id);
        if (bookDTO.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String coverImageUrl = bookDTO.get().getCoverImageUrl();
        if (coverImageUrl == null || coverImageUrl.isEmpty()) {
            if (placeholder) {
                // TODO: Return placeholder image when implemented
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = fileStorageService.loadImageAsResource(coverImageUrl);
            String contentType = fileStorageService.getImageContentType(coverImageUrl);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (IOException e) {
            LOG.error("Failed to load book cover image: {}", coverImageUrl, e);
            if (placeholder) {
                // TODO: Return placeholder image when implemented
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.notFound().build();
        }
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

    /**
     * {@code POST  /books/:id/tags/add} : Add tags to a book.
     *
     * @param id the id of the book.
     * @param tagIds the list of tag IDs to add.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated bookDTO.
     */
    @PostMapping("/{id}/tags/add")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookDTO> addTagsToBook(
        @PathVariable("id") Long id,
        @RequestBody List<Long> tagIds
    ) {
        LOG.debug("REST request to add tags {} to Book : {}", tagIds, id);
        BookDTO result = bookService.addTagsToBook(id, tagIds);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code POST  /books/:id/tags/remove} : Remove tags from a book.
     *
     * @param id the id of the book.
     * @param tagIds the list of tag IDs to remove.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated bookDTO.
     */
    @PostMapping("/{id}/tags/remove")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookDTO> removeTagsFromBook(
        @PathVariable("id") Long id,
        @RequestBody List<Long> tagIds
    ) {
        LOG.debug("REST request to remove tags {} from Book : {}", tagIds, id);
        BookDTO result = bookService.removeTagsFromBook(id, tagIds);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code GET  /books/:id/recommendations} : get recommended book packs for the "id" book.
     *
     * @param id the id of the book to get recommendations for.
     * @param pageable the pagination information (default limit 5).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of book packs in body.
     */
    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<BookPackDTO>> getBookRecommendations(
        @PathVariable("id") Long id,
        @ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get recommendations for Book : {}", id);

        // Default to 5 recommendations if not specified
        Pageable limitedPageable = pageable.isPaged() ? pageable : Pageable.ofSize(5);

        Page<BookPackDTO> page = bookPackService.findRecommendationsForBook(id, limitedPageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
