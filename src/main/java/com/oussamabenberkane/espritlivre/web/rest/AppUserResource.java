package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.AppUserService;
import com.oussamabenberkane.espritlivre.service.FileStorageService;
import com.oussamabenberkane.espritlivre.service.dto.AppUserDTO;
import com.oussamabenberkane.espritlivre.service.dto.EmailChangeDTO;
import com.oussamabenberkane.espritlivre.service.dto.ProfileUpdateResponseDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing App Users.
 */
@RestController
@RequestMapping("/api/app-users")
public class AppUserResource {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserResource.class);

    private final AppUserService appUserService;

    private final FileStorageService fileStorageService;

    public AppUserResource(AppUserService appUserService, FileStorageService fileStorageService) {
        this.appUserService = appUserService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * {@code POST  /app-users/register} : Register a new app user (complete profile after OAuth2 login).
     *
     * @param appUserDTO the user information to register.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/register")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> registerAppUser(@Valid @RequestBody AppUserDTO appUserDTO) {
        LOG.debug("REST request to register app user : {}", appUserDTO);

        appUserService.completeAppUserRegistration(appUserDTO);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code GET  /app-users/profile} : Get current user profile.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the user profile in body.
     */
    @GetMapping("/profile")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppUserDTO> getProfile() {
        LOG.debug("REST request to get current user profile");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        AppUserDTO profile = appUserService.getAppUserProfile(login);
        return ResponseEntity.ok(profile);
    }

    /**
     * {@code PUT  /app-users/profile} : Update current user profile.
     *
     * @param appUserDTO the updated profile information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and linked/updated orders counts in body.
     */
    @PutMapping("/profile")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileUpdateResponseDTO> updateProfile(@Valid @RequestBody AppUserDTO appUserDTO) {
        LOG.debug("REST request to update user profile : {}", appUserDTO);

        int[] counts = appUserService.updateAppUserProfile(appUserDTO);
        ProfileUpdateResponseDTO response = new ProfileUpdateResponseDTO(counts[0], counts[1]);
        return ResponseEntity.ok(response);
    }

    /**
     * {@code POST  /app-users/change-email} : Request email change (sends verification email).
     *
     * @param emailChangeDTO the new email information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/change-email")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changeEmail(@Valid @RequestBody EmailChangeDTO emailChangeDTO) {
        LOG.debug("REST request to change email to: {}", emailChangeDTO.getNewEmail());

        appUserService.requestEmailChange(emailChangeDTO.getNewEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * {@code GET  /app-users/verify-email} : Verify new email with token.
     *
     * @param token the verification token.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        LOG.debug("REST request to verify email with token");

        appUserService.verifyEmailChange(token);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code DELETE  /app-users/account} : Delete current user account (soft delete).
     *
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/account")
    //@PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAccount() {
        LOG.debug("REST request to delete user account");

        appUserService.deleteUserAccount();
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET  /app-users/{id}/picture} : get the profile picture for the "id" user.
     * This endpoint is publicly accessible without authentication.
     * Returns a default placeholder image if the picture is not found or fails to load.
     *
     * @param id the id of the user.
     * @return the {@link ResponseEntity} with the image file.
     */
    @GetMapping("/{id}/picture")
    public ResponseEntity<Resource> getUserPicture(@PathVariable("id") String id) {
        LOG.debug("REST request to get User picture : {}", id);

        AppUserDTO userDTO = appUserService.getAppUserProfile(id);
        if (userDTO == null) {
            return loadPlaceholder();
        }

        String imageUrl = userDTO.getImageUrl();
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
            LOG.error("Failed to load user picture: {}, returning placeholder", imageUrl, e);
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
}
