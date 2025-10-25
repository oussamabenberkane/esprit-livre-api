package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.repository.BookPackRepository;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.service.BookPackService;
import com.oussamabenberkane.espritlivre.service.dto.BookPackDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
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
 * REST controller for managing {@link com.oussamabenberkane.espritlivre.domain.BookPack}.
 */
@RestController
@RequestMapping("/api/book-packs")
public class BookPackResource {

    private static final Logger LOG = LoggerFactory.getLogger(BookPackResource.class);

    private static final String ENTITY_NAME = "bookPack";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final BookPackService bookPackService;

    private final BookPackRepository bookPackRepository;

    public BookPackResource(BookPackService bookPackService, BookPackRepository bookPackRepository) {
        this.bookPackService = bookPackService;
        this.bookPackRepository = bookPackRepository;
    }

    /**
     * {@code POST  /book-packs} : Create a new bookPack with cover image.
     *
     * @param bookPackDTO the book pack data.
     * @param coverImage the cover image file (required).
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new bookPackDTO, or with status {@code 400 (Bad Request)} if the bookPack has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping(value = "", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookPackDTO> createBookPack(
        @RequestPart("bookPack") @Valid BookPackDTO bookPackDTO,
        @RequestPart("coverImage") @NotNull MultipartFile coverImage
    ) throws URISyntaxException {
        LOG.debug("REST request to save BookPack with cover image : {}", bookPackDTO);

        if (bookPackDTO.getId() != null) {
            throw new BadRequestAlertException("A new bookPack cannot already have an ID", ENTITY_NAME, "idexists");
        }

        BookPackDTO result = bookPackService.saveWithCover(bookPackDTO, coverImage, false);
        return ResponseEntity.created(new URI("/api/book-packs/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /book-packs/:id} : Updates an existing bookPack with optional cover image.
     *
     * @param id the id of the book pack to update.
     * @param bookPackDTO the book pack data.
     * @param coverImage the cover image file (optional for updates).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated bookPackDTO,
     * or with status {@code 400 (Bad Request)} if the bookPackDTO is not valid.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<BookPackDTO> updateBookPack(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestPart("bookPack") @Valid BookPackDTO bookPackDTO,
        @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) throws URISyntaxException {
        LOG.debug("REST request to update BookPack : {}, {}", id, bookPackDTO);

        if (bookPackDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, bookPackDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!bookPackRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        BookPackDTO result = bookPackService.saveWithCover(bookPackDTO, coverImage, true);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /book-packs} : get all the bookPacks with books.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of bookPacks in body.
     */
    @GetMapping("")
    public ResponseEntity<Page<BookPackDTO>> getAllBookPacks(@ParameterObject Pageable pageable) {
        LOG.debug("REST request to get a page of BookPacks");
        Page<BookPackDTO> page = bookPackService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page);
    }

    /**
     * {@code GET  /book-packs/:id} : get the "id" bookPack.
     *
     * @param id the id of the bookPackDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the bookPackDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookPackDTO> getBookPack(@PathVariable("id") Long id) {
        LOG.debug("REST request to get BookPack : {}", id);
        Optional<BookPackDTO> bookPackDTO = bookPackService.findOne(id);
        return ResponseUtil.wrapOrNotFound(bookPackDTO);
    }

    /**
     * {@code DELETE  /book-packs/:id} : delete the "id" bookPack.
     *
     * @param id the id of the bookPackDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteBookPack(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete BookPack : {}", id);
        bookPackService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
