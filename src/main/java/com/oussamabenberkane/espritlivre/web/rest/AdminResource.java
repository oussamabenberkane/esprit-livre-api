package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.AppUserService;
import com.oussamabenberkane.espritlivre.service.FileStorageService;
import com.oussamabenberkane.espritlivre.service.dto.AppUserDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for admin-only operations.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdminResource.class);

    private final AppUserService appUserService;

    private final FileStorageService fileStorageService;

    public AdminResource(AppUserService appUserService, FileStorageService fileStorageService) {
        this.appUserService = appUserService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * {@code GET  /admin/users} : Get all non-admin users (admin only).
     *
     * @param active Optional filter for active/inactive users (null = all users).
     * @param pageable Pagination and sorting information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of users in body.
     */
    @GetMapping("/users")
    public ResponseEntity<Page<AppUserDTO>> getAllUsers(
        @RequestParam(required = false) Boolean active,
        Pageable pageable
    ) {
        LOG.debug("REST request to get all non-admin users with active filter: {}", active);
        Page<AppUserDTO> page = appUserService.getAllNonAdminUsers(active, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * {@code PATCH  /admin/users/{id}/toggle} : Toggle user activation status (admin only).
     *
     * @param id the id of the user to toggle.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<Void> toggleUserActivation(@PathVariable String id) {
        LOG.debug("REST request to toggle user activation for ID: {}", id);
        appUserService.toggleUserActivation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET  /admin/users/export} : Export all non-admin users to Excel (admin only).
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the Excel file as a byte array.
     */
    @GetMapping("/users/export")
    public ResponseEntity<Resource> exportUsers() {
        LOG.debug("REST request to export all non-admin users to Excel");

        try {
            byte[] excelData = appUserService.exportUsersToExcel();

            // Generate filename with current timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "users_export_" + timestamp + ".xlsx";

            ByteArrayResource resource = new ByteArrayResource(excelData);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelData.length)
                .body(resource);
        } catch (IOException e) {
            LOG.error("Failed to export users to Excel", e);
            throw new BadRequestAlertException("Failed to export users", "admin", "exportfailed");
        }
    }

    /**
     * {@code GET  /admin/profile} : Get current admin profile.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the admin profile in body.
     */
    @GetMapping("/profile")
    public ResponseEntity<AppUserDTO> getAdminProfile() {
        LOG.debug("REST request to get current admin profile");

        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not found", "admin", "usernotfound"));

        AppUserDTO profile = appUserService.getAppUserProfile(login);
        return ResponseEntity.ok(profile);
    }

    /**
     * {@code PUT  /admin/profile} : Update current admin profile.
     * Admin can only update firstName, lastName, email, and imageUrl.
     *
     * @param appUserDTO the updated profile information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/profile")
    public ResponseEntity<Void> updateAdminProfile(@Valid @RequestBody AppUserDTO appUserDTO) {
        LOG.debug("REST request to update admin profile : {}", appUserDTO);

        appUserService.updateAdminProfile(appUserDTO);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code GET  /admin/picture} : Get the current admin's profile picture.
     * This endpoint requires admin authentication.
     *
     * @return the {@link ResponseEntity} with the image file.
     */
    @GetMapping("/picture")
    public ResponseEntity<Resource> getAdminPicture() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not found", "admin", "usernotfound"));

        LOG.debug("REST request to get admin picture for : {}", login);

        AppUserDTO adminDTO = appUserService.getAppUserProfile(login);
        if (adminDTO == null) {
            return loadPlaceholder();
        }

        String imageUrl = adminDTO.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            return loadPlaceholder();
        }

        try {
            Resource resource = fileStorageService.loadImageAsResource(imageUrl);
            String contentType = fileStorageService.getImageContentType(imageUrl);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (IOException e) {
            LOG.error("Failed to load admin picture: {}, returning placeholder", imageUrl, e);
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
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"default.png\"")
                .body(resource);
        } catch (IOException e) {
            LOG.error("Failed to load placeholder image", e);
            return ResponseEntity.notFound().build();
        }
    }
}
