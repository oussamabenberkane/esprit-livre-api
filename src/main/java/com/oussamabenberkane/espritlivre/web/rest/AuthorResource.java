package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.repository.AuthorRepository;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.service.AuthorService;
import com.oussamabenberkane.espritlivre.service.FileStorageService;
import com.oussamabenberkane.espritlivre.service.dto.AuthorDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.oussamabenberkane.espritlivre.domain.Author}.
 */
@RestController
@RequestMapping("/api/authors")
public class AuthorResource {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorResource.class);

    private static final String ENTITY_NAME = "author";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AuthorService authorService;

    private final AuthorRepository authorRepository;

    private final FileStorageService fileStorageService;

    public AuthorResource(AuthorService authorService, AuthorRepository authorRepository, FileStorageService fileStorageService) {
        this.authorService = authorService;
        this.authorRepository = authorRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * {@code POST  /authors} : Create a new author with optional profile picture.
     *
     * @param authorDTO the author data.
     * @param profilePicture the author profile picture file (optional).
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new authorDTO, or with status {@code 400 (Bad Request)} if the author has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping(value = "", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<AuthorDTO> createAuthor(
        @RequestPart("author") @Valid AuthorDTO authorDTO,
        @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture
    ) throws URISyntaxException {
        LOG.debug("REST request to save Author with profile picture : {}", authorDTO);

        if (authorDTO.getId() != null) {
            throw new BadRequestAlertException("A new author cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (authorDTO.getName() == null || authorDTO.getName().trim().isEmpty()) {
            throw new BadRequestAlertException("Author name is required", ENTITY_NAME, "namerequired");
        }

        AuthorDTO result = authorService.saveWithPicture(authorDTO, profilePicture, false);
        return ResponseEntity.created(new URI("/api/authors/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /authors/:id} : Updates an existing author with optional profile picture.
     *
     * @param id the id of the author to update.
     * @param authorDTO the author data.
     * @param profilePicture the author profile picture file (optional for updates).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated authorDTO,
     * or with status {@code 400 (Bad Request)} if the authorDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the authorDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<AuthorDTO> updateAuthor(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestPart("author") @Valid AuthorDTO authorDTO,
        @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture
    ) throws URISyntaxException {
        LOG.debug("REST request to update Author : {}, {}", id, authorDTO);

        if (authorDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, authorDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!authorRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        AuthorDTO result = authorService.saveWithPicture(authorDTO, profilePicture, true);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /authors} : get all the authors with pagination and search.
     *
     * @param pageable the pagination information.
     * @param search the search term for author name (optional).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of authors in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AuthorDTO>> getAllAuthors(
        @ParameterObject Pageable pageable,
        @RequestParam(name = "search", required = false) String search
    ) {
        LOG.debug("REST request to get a page of Authors with search: {}", search);
        Page<AuthorDTO> page = authorService.findAllWithFilters(pageable, search);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /authors/top} : get top 10 authors with most books.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of top authors in body.
     */
    @GetMapping("/top")
    public ResponseEntity<List<AuthorDTO>> getTopAuthors() {
        LOG.debug("REST request to get top 10 authors by book count");
        List<AuthorDTO> topAuthors = authorService.findTop10AuthorsByBookCount();
        return ResponseEntity.ok().body(topAuthors);
    }

    /**
     * {@code GET  /authors/:id} : get the "id" author.
     *
     * @param id the id of the authorDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the authorDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuthorDTO> getAuthor(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Author : {}", id);
        Optional<AuthorDTO> authorDTO = authorService.findOne(id);
        return ResponseUtil.wrapOrNotFound(authorDTO);
    }

    /**
     * {@code GET  /authors/:id/picture} : get the profile picture for the "id" author.
     * This endpoint is publicly accessible without authentication.
     * Returns a default placeholder image if the picture is not found or fails to load.
     *
     * @param id the id of the author.
     * @return the {@link ResponseEntity} with the image file.
     */
    @GetMapping("/{id}/picture")
    public ResponseEntity<Resource> getAuthorPicture(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Author picture : {}", id);

        Optional<AuthorDTO> authorDTO = authorService.findOne(id);
        if (authorDTO.isEmpty()) {
            return loadPlaceholder();
        }

        String profilePictureUrl = authorDTO.orElseThrow().getProfilePictureUrl();
        if (profilePictureUrl == null || profilePictureUrl.isEmpty()) {
            return loadPlaceholder();
        }

        try {
            Resource resource = fileStorageService.loadImageAsResource(profilePictureUrl);
            String contentType = fileStorageService.getImageContentType(profilePictureUrl);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (IOException e) {
            LOG.error("Failed to load author picture: {}, returning placeholder", profilePictureUrl, e);
            return loadPlaceholder();
        }
    }

    /**
     * Load and return the default placeholder image.
     *
     * @return the {@link ResponseEntity} with the placeholder image.
     */
    private ResponseEntity<Resource> loadPlaceholder() {
        try {
            Resource resource = fileStorageService.loadPlaceholderImage();

            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"default.png\"")
                .body(resource);
        } catch (IOException e) {
            LOG.error("Failed to load placeholder image", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * {@code DELETE  /authors/:id} : delete the "id" author.
     *
     * @param id the id of the authorDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteAuthor(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Author : {}", id);
        authorService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
