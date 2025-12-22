package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.security.AuthoritiesConstants;
import com.oussamabenberkane.espritlivre.service.LikeService;
import com.oussamabenberkane.espritlivre.service.dto.LikeToggleResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing {@link com.oussamabenberkane.espritlivre.domain.Like}.
 */
@RestController
@RequestMapping("/api/likes")
public class LikeResource {

    private static final Logger LOG = LoggerFactory.getLogger(LikeResource.class);


    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final LikeService likeService;

    public LikeResource(LikeService likeService) {
        this.likeService = likeService;
    }

    /**
     * {@code POST  /likes/toggle/:bookId} : Toggle like for a book.
     *
     * @param bookId the id of the book to like/unlike.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the toggle response in body.
     */
    @PostMapping("/toggle/{bookId}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.USER + "\")")
    public ResponseEntity<LikeToggleResponseDTO> toggleLike(@PathVariable("bookId") Long bookId) {
        LOG.debug("REST request to toggle like for book : {}", bookId);
        LikeToggleResponseDTO response = likeService.toggleLike(bookId);
        return ResponseEntity.ok(response);
    }
}
