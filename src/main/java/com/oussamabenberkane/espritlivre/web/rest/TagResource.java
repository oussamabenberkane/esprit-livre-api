package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import com.oussamabenberkane.espritlivre.repository.TagRepository;
import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.service.FileStorageService;
import com.oussamabenberkane.espritlivre.service.TagService;
import com.oussamabenberkane.espritlivre.service.dto.TagDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
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
 * REST controller for managing {@link com.oussamabenberkane.espritlivre.domain.Tag}.
 */
@RestController
@RequestMapping("/api/tags")
public class TagResource {

    private static final Logger LOG = LoggerFactory.getLogger(TagResource.class);

    private static final String ENTITY_NAME = "tag";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final TagService tagService;

    private final TagRepository tagRepository;

    private final FileStorageService fileStorageService;

    public TagResource(TagService tagService, TagRepository tagRepository, FileStorageService fileStorageService) {
        this.tagService = tagService;
        this.tagRepository = tagRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * {@code POST  /tags} : Create a new tag with optional image.
     *
     * @param tagDTO the tag data.
     * @param image the category image file (required for CATEGORY tags).
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new tagDTO, or with status {@code 400 (Bad Request)} if the tag has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping(value = "", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<TagDTO> createTag(
        @RequestPart("tag") @Valid TagDTO tagDTO,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) throws URISyntaxException {
        LOG.debug("REST request to save Tag with image : {}", tagDTO);

        if (tagDTO.getId() != null) {
            throw new BadRequestAlertException("A new tag cannot already have an ID", ENTITY_NAME, "idexists");
        }

        TagDTO result = tagService.saveWithImage(tagDTO, image, false);
        return ResponseEntity.created(new URI("/api/tags/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /tags/:id} : Updates an existing tag with optional image.
     *
     * @param id the id of the tag to update.
     * @param tagDTO the tag data.
     * @param image the category image file (optional for updates).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tagDTO,
     * or with status {@code 400 (Bad Request)} if the tagDTO is not valid,
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<TagDTO> updateTag(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestPart("tag") @Valid TagDTO tagDTO,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) throws URISyntaxException {
        LOG.debug("REST request to update Tag : {}, {}", id, tagDTO);

        if (tagDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, tagDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!tagRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        TagDTO result = tagService.saveWithImage(tagDTO, image, true);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /tags} : get all active tags with pagination and search.
     *
     * @param pageable the pagination information.
     * @param type the tag type filter (optional).
     * @param search the search term for tag names (optional).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the page of tags in body.
     */
    @GetMapping("")
    public ResponseEntity<List<TagDTO>> getAllTags(
        Pageable pageable,
        @RequestParam(name = "type", required = false) TagType type,
        @RequestParam(name = "search", required = false) String search
    ) {
        LOG.debug("REST request to get all Tags with type: {}, search: {}", type, search);
        Page<TagDTO> page = tagService.findAllWithFilters(pageable, type, search);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /tags/:id} : get the "id" tag.
     *
     * @param id the id of the tagDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the tagDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TagDTO> getTag(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Tag : {}", id);
        Optional<TagDTO> tagDTO = tagService.findOne(id);
        return ResponseUtil.wrapOrNotFound(tagDTO);
    }

    /**
     * {@code GET  /tags/:id/image} : get the image for the "id" tag (category).
     * This endpoint is publicly accessible without authentication.
     * Returns a default placeholder image if the image is not found or fails to load.
     *
     * @param id the id of the tag.
     * @return the {@link ResponseEntity} with the image file.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getTagImage(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Tag image : {}", id);

        Optional<TagDTO> tagDTO = tagService.findOne(id);
        if (tagDTO.isEmpty()) {
            return loadPlaceholder();
        }

        String imageUrl = tagDTO.get().getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            return loadPlaceholder();
        }

        try {
            Resource resource = fileStorageService.loadImageAsResource(imageUrl);
            String contentType = fileStorageService.getImageContentType(imageUrl);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (IOException e) {
            LOG.error("Failed to load tag image: {}, returning placeholder", imageUrl, e);
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
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"default.png\"")
                .body(resource);
        } catch (IOException e) {
            LOG.error("Failed to load placeholder image", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * {@code DELETE  /tags/:id} : delete the "id" tag.
     *
     * @param id the id of the tagDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteTag(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Tag : {}", id);
        tagService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code POST  /tags/:id/books/add} : Add books to a tag.
     *
     * @param id the id of the tag.
     * @param bookIds the list of book IDs to add.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tagDTO.
     */
    @PostMapping("/{id}/books/add")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<TagDTO> addBooksToTag(
        @PathVariable("id") Long id,
        @RequestBody List<Long> bookIds
    ) {
        LOG.debug("REST request to add books {} to Tag : {}", bookIds, id);
        TagDTO result = tagService.addBooksToTag(id, bookIds);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code POST  /tags/:id/books/remove} : Remove books from a tag.
     *
     * @param id the id of the tag.
     * @param bookIds the list of book IDs to remove.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tagDTO.
     */
    @PostMapping("/{id}/books/remove")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<TagDTO> removeBooksFromTag(
        @PathVariable("id") Long id,
        @RequestBody List<Long> bookIds
    ) {
        LOG.debug("REST request to remove books {} from Tag : {}", bookIds, id);
        TagDTO result = tagService.removeBooksFromTag(id, bookIds);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code POST  /tags/:id/change-color} : Change the color of an ETIQUETTE tag.
     *
     * @param id the id of the tag.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated tagDTO.
     */
    @PostMapping("/{id}/change-color")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<TagDTO> changeTagColor(@PathVariable("id") Long id) {
        LOG.debug("REST request to change color for Tag : {}", id);
        TagDTO result = tagService.changeColor(id);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }
}
