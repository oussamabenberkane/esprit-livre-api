package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.repository.AuthorRepository;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.service.AuthorService;
import com.oussamabenberkane.espritlivre.service.dto.AuthorDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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

    public AuthorResource(AuthorService authorService, AuthorRepository authorRepository) {
        this.authorService = authorService;
        this.authorRepository = authorRepository;
    }

    /**
     * {@code POST  /authors} : Create a new author.
     *
     * @param authorDTO the authorDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new authorDTO, or with status {@code 400 (Bad Request)} if the author has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<AuthorDTO> createAuthor(@Valid @RequestBody AuthorDTO authorDTO) throws URISyntaxException {
        LOG.debug("REST request to save Author : {}", authorDTO);
        if (authorDTO.getId() != null) {
            throw new BadRequestAlertException("A new author cannot already have an ID", ENTITY_NAME, "idexists");
        }
        if (authorDTO.getName() == null || authorDTO.getName().trim().isEmpty()) {
            throw new BadRequestAlertException("Author name is required", "author", "namerequired");
        }
        authorDTO = authorService.findOrCreateByName(authorDTO.getName());
        return ResponseEntity.created(new URI("/api/authors/" + authorDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, authorDTO.getId().toString()))
            .body(authorDTO);
    }

    /**
     * {@code PUT  /authors/:id} : Updates an existing author.
     *
     * @param id the id of the authorDTO to save.
     * @param authorDTO the authorDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated authorDTO,
     * or with status {@code 400 (Bad Request)} if the authorDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the authorDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<AuthorDTO> updateAuthor(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody AuthorDTO authorDTO
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

        authorDTO = authorService.update(authorDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, authorDTO.getId().toString()))
            .body(authorDTO);
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
