package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.repository.LikeRepository;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.service.LikeService;
import com.oussamabenberkane.espritlivre.service.dto.LikeDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * REST controller for managing {@link com.oussamabenberkane.espritlivre.domain.Like}.
 */
@RestController
@RequestMapping("/api/likes")
public class LikeResource {

    private static final Logger LOG = LoggerFactory.getLogger(LikeResource.class);

    private static final String ENTITY_NAME = "like";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final LikeService likeService;

    private final LikeRepository likeRepository;

    public LikeResource(LikeService likeService, LikeRepository likeRepository) {
        this.likeService = likeService;
        this.likeRepository = likeRepository;
    }

    /**
     * {@code POST  /likes} : Create a new like.
     *
     * @param likeDTO the likeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new likeDTO, or with status {@code 400 (Bad Request)} if the like has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<LikeDTO> createLike(@RequestBody LikeDTO likeDTO) throws URISyntaxException {
        LOG.debug("REST request to save Like : {}", likeDTO);
        if (likeDTO.getId() != null) {
            throw new BadRequestAlertException("A new like cannot already have an ID", ENTITY_NAME, "idexists");
        }
        likeDTO = likeService.save(likeDTO);
        return ResponseEntity.created(new URI("/api/likes/" + likeDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, likeDTO.getId().toString()))
            .body(likeDTO);
    }

    /**
     * {@code PUT  /likes/:id} : Updates an existing like.
     *
     * @param id the id of the likeDTO to save.
     * @param likeDTO the likeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated likeDTO,
     * or with status {@code 400 (Bad Request)} if the likeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the likeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<LikeDTO> updateLike(@PathVariable(value = "id", required = false) final Long id, @RequestBody LikeDTO likeDTO)
        throws URISyntaxException {
        LOG.debug("REST request to update Like : {}, {}", id, likeDTO);
        if (likeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, likeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!likeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        likeDTO = likeService.update(likeDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, likeDTO.getId().toString()))
            .body(likeDTO);
    }

    /**
     * {@code GET  /likes} : get all the likes.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of likes in body.
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<List<LikeDTO>> getAllLikes(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of Likes");
        Page<LikeDTO> page;
        if (eagerload) {
            page = likeService.findAllWithEagerRelationships(pageable);
        } else {
            page = likeService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /likes/:id} : get the "id" like.
     *
     * @param id the id of the likeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the likeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<LikeDTO> getLike(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Like : {}", id);
        Optional<LikeDTO> likeDTO = likeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(likeDTO);
    }

    /**
     * {@code DELETE  /likes/:id} : delete the "id" like.
     *
     * @param id the id of the likeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<Void> deleteLike(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Like : {}", id);
        likeService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
