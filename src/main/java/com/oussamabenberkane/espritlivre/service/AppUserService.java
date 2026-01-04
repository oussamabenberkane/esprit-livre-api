package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.repository.UserRepository;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.dto.AppUserDTO;
import com.oussamabenberkane.espritlivre.service.dto.PasswordChangeDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class AppUserService {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserService.class);

    private final UserRepository userRepository;
    private final MailService mailService;
    private final CacheManager cacheManager;
    private final OrderService orderService;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;
    private final ApplicationProperties applicationProperties;

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String keycloakIssuerUri;

    public AppUserService(
        UserRepository userRepository,
        MailService mailService,
        CacheManager cacheManager,
        OrderService orderService,
        FileStorageService fileStorageService,
        RestTemplate restTemplate,
        ApplicationProperties applicationProperties
    ) {
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.cacheManager = cacheManager;
        this.orderService = orderService;
        this.fileStorageService = fileStorageService;
        this.restTemplate = restTemplate;
        this.applicationProperties = applicationProperties;
    }

    public void completeAppUserRegistration(AppUserDTO appUserDTO) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "appUser", "notauthenticated"));

        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        user.setLastName(appUserDTO.getLastName());
        user.setEmail(appUserDTO.getEmail());
        user.setPendingEmail(appUserDTO.getEmail());
        user.setPhone(appUserDTO.getPhone());
        user.setWilaya(appUserDTO.getWilaya());
        user.setCity(appUserDTO.getCity());
        user.setStreetAddress(appUserDTO.getStreetAddress());
        user.setPostalCode(appUserDTO.getPostalCode());
        user.setDefaultShippingMethod(appUserDTO.getDefaultShippingMethod());
        user.setDefaultShippingProvider(appUserDTO.getDefaultShippingProvider());

        if (appUserDTO.getFirstName() != null) {
            user.setFirstName(appUserDTO.getFirstName());
        }
        if (appUserDTO.getLangKey() != null) {
            user.setLangKey(appUserDTO.getLangKey());
        }

        userRepository.save(user);
        clearUserCaches(user);
        mailService.sendNewUserRegistrationEmailToAdmin(user);
    }

    @Transactional(readOnly = true)
    public AppUserDTO getAppUserProfile(String login) {
        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));
        return mapToDTO(user);
    }

    public int[] updateAppUserProfile(AppUserDTO appUserDTO) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "appUser", "notauthenticated"));

        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        // Track old phone to detect changes
        String oldPhone = user.getPhone();
        int linkedOrdersCount = 0;
        int updatedOrdersCount = 0;

        if (appUserDTO.getFirstName() != null) user.setFirstName(appUserDTO.getFirstName());
        if (appUserDTO.getLastName() != null) user.setLastName(appUserDTO.getLastName());
        if (appUserDTO.getPhone() != null) user.setPhone(appUserDTO.getPhone());
        if (appUserDTO.getWilaya() != null) user.setWilaya(appUserDTO.getWilaya());
        if (appUserDTO.getCity() != null) user.setCity(appUserDTO.getCity());
        if (appUserDTO.getStreetAddress() != null) user.setStreetAddress(appUserDTO.getStreetAddress());
        if (appUserDTO.getPostalCode() != null) user.setPostalCode(appUserDTO.getPostalCode());
        if (appUserDTO.getDefaultShippingMethod() != null) user.setDefaultShippingMethod(appUserDTO.getDefaultShippingMethod());
        if (appUserDTO.getDefaultShippingProvider() != null) user.setDefaultShippingProvider(appUserDTO.getDefaultShippingProvider());
        if (appUserDTO.getLangKey() != null) user.setLangKey(appUserDTO.getLangKey());
        if (appUserDTO.getImageUrl() != null) user.setImageUrl(appUserDTO.getImageUrl());

        userRepository.save(user);
        clearUserCaches(user);

        // Check if phone number changed
        String newPhone = user.getPhone();
        boolean phoneChanged = !Objects.equals(
            OrderService.normalizePhoneNumber(oldPhone),
            OrderService.normalizePhoneNumber(newPhone)
        );

        if (phoneChanged && newPhone != null && !newPhone.isEmpty()) {
            LOG.info("Phone number changed for user '{}', updating existing orders and linking guest orders", login);

            // Update phone number on existing active orders
            updatedOrdersCount = orderService.updateUserActiveOrdersPhone(user);

            // Link guest orders with matching phone
            linkedOrdersCount = orderService.linkGuestOrdersToUser(user.getId(), newPhone);
        }

        return new int[] { linkedOrdersCount, updatedOrdersCount };
    }

    /**
     * Update admin profile (admin only).
     * Admin can only update firstName, lastName, email, and profile picture.
     * Profile picture is always stored as "admin.{extension}".
     * Does not handle order linking.
     * Synchronizes firstName, lastName, and email with Keycloak.
     *
     * @param appUserDTO the updated profile information
     * @param profilePicture the optional profile picture file
     */
    public void updateAdminProfile(AppUserDTO appUserDTO, MultipartFile profilePicture) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "admin", "notauthenticated"));

        User admin = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "admin", "usernotfound"));

        // Get the current user's ID from JWT token for Keycloak updates
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new BadRequestAlertException("Invalid authentication", "admin", "invalidauth");
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();
        String userId = jwt.getSubject(); // Keycloak user ID

        // Track if we need to sync to Keycloak
        boolean needsKeycloakSync = false;
        Map<String, Object> keycloakUpdates = new HashMap<>();

        // Only update firstName, lastName, and email
        if (appUserDTO.getFirstName() != null && !appUserDTO.getFirstName().equals(admin.getFirstName())) {
            admin.setFirstName(appUserDTO.getFirstName());
            keycloakUpdates.put("firstName", appUserDTO.getFirstName());
            needsKeycloakSync = true;
        }
        if (appUserDTO.getLastName() != null && !appUserDTO.getLastName().equals(admin.getLastName())) {
            admin.setLastName(appUserDTO.getLastName());
            keycloakUpdates.put("lastName", appUserDTO.getLastName());
            needsKeycloakSync = true;
        }
        if (appUserDTO.getEmail() != null && !appUserDTO.getEmail().equalsIgnoreCase(admin.getEmail())) {
            // Check if email already exists in local database
            userRepository.findOneByEmailIgnoreCase(appUserDTO.getEmail()).ifPresent(existingUser -> {
                if (!existingUser.getLogin().equals(login)) {
                    throw new BadRequestAlertException("Email already in use", "admin", "emailexists");
                }
            });

            admin.setEmail(appUserDTO.getEmail());
            keycloakUpdates.put("email", appUserDTO.getEmail());
            keycloakUpdates.put("emailVerified", true); // Maintain email verification status
            needsKeycloakSync = true;
        }

        // Handle profile picture upload if provided
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                // Always delete all existing admin pictures (with any extension)
                fileStorageService.deleteAllAdminPictures();

                // Store new admin profile picture with fixed filename "admin.{extension}"
                String imageUrl = fileStorageService.storeAdminPicture(profilePicture);
                admin.setImageUrl(imageUrl);
                LOG.debug("Admin profile picture uploaded as '{}'", imageUrl);
            } catch (IOException e) {
                LOG.error("Failed to store admin profile picture", e);
                throw new BadRequestAlertException("Failed to upload profile picture", "admin", "imageuploadfailed");
            }
        }

        // Sync to Keycloak if any fields changed
        if (needsKeycloakSync) {
            try {
                syncAdminProfileToKeycloak(userId, keycloakUpdates);
                LOG.info("Admin profile synchronized to Keycloak for user '{}'", login);
            } catch (Exception e) {
                LOG.error("Failed to sync admin profile to Keycloak for user '{}'", login, e);
                throw new BadRequestAlertException("Failed to sync profile to Keycloak", "admin", "keycloaksyncfailed");
            }
        }

        User savedAdmin = userRepository.save(admin);
        LOG.info("Admin profile saved '{}'", savedAdmin);
        clearUserCaches(savedAdmin);

        LOG.info("Admin profile updated for user '{}'", login);
    }

    /**
     * Synchronize admin profile changes to Keycloak.
     *
     * @param userId the Keycloak user ID
     * @param updates the map of fields to update (firstName, lastName, email, emailVerified)
     */
    private void syncAdminProfileToKeycloak(String userId, Map<String, Object> updates) {
        String keycloakBaseUrl = keycloakIssuerUri.replace("/realms/jhipster", "");

        // Step 1: Get Keycloak master admin access token
        String masterTokenEndpoint = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders masterHeaders = new HttpHeaders();
        masterHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String masterTokenBody = "grant_type=password" +
            "&client_id=" + applicationProperties.getKeycloak().getAdminClientId() +
            "&username=" + applicationProperties.getKeycloak().getAdminUsername() +
            "&password=" + applicationProperties.getKeycloak().getAdminPassword();

        HttpEntity<String> masterTokenRequest = new HttpEntity<>(masterTokenBody, masterHeaders);

        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> masterTokenResponse = restTemplate.postForEntity(
                masterTokenEndpoint,
                masterTokenRequest,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> masterTokenData = masterTokenResponse.getBody();
            String masterAccessToken = (String) masterTokenData.get("access_token");

            LOG.debug("Obtained Keycloak master admin token for profile sync");

            // Step 2: Check if email already exists in Keycloak (if email is being updated)
            if (updates.containsKey("email")) {
                String email = (String) updates.get("email");
                String searchUrl = keycloakBaseUrl + "/admin/realms/jhipster/users?email=" + email + "&exact=true";

                HttpHeaders searchHeaders = new HttpHeaders();
                searchHeaders.setBearerAuth(masterAccessToken);
                HttpEntity<Void> searchRequest = new HttpEntity<>(searchHeaders);

                @SuppressWarnings("rawtypes")
                ResponseEntity<List> searchResponse = restTemplate.exchange(
                    searchUrl,
                    HttpMethod.GET,
                    searchRequest,
                    List.class
                );

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> existingUsers = searchResponse.getBody();
                if (existingUsers != null && !existingUsers.isEmpty()) {
                    // Check if the found user is not the current user
                    for (Map<String, Object> user : existingUsers) {
                        String existingUserId = (String) user.get("id");
                        if (!existingUserId.equals(userId)) {
                            throw new BadRequestAlertException("Email already exists in Keycloak", "admin", "emailexists");
                        }
                    }
                }
            }

            // Step 3: Update user profile in Keycloak
            String updateUserUrl = keycloakBaseUrl + "/admin/realms/jhipster/users/" + userId;

            HttpHeaders updateHeaders = new HttpHeaders();
            updateHeaders.setContentType(MediaType.APPLICATION_JSON);
            updateHeaders.setBearerAuth(masterAccessToken);

            HttpEntity<Map<String, Object>> updateRequest = new HttpEntity<>(updates, updateHeaders);

            // PUT request to update user
            restTemplate.exchange(
                updateUserUrl,
                HttpMethod.PUT,
                updateRequest,
                Void.class
            );

            LOG.debug("Successfully updated Keycloak user profile for userId: {}", userId);

        } catch (HttpClientErrorException e) {
            LOG.error("Error syncing profile to Keycloak, status: {}, error: {}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new BadRequestAlertException("Email already exists in Keycloak", "admin", "emailexists");
            }
            throw e;
        }
    }

    public void requestEmailChange(String newEmail) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "appUser", "notauthenticated"));

        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        userRepository.findOneByEmailIgnoreCase(newEmail).ifPresent(existingUser -> {
            if (!existingUser.getLogin().equals(login)) {
                throw new BadRequestAlertException("Email already in use", "appUser", "emailexists");
            }
        });

        String token = RandomStringUtils.secure().toString();
        user.setEmailVerificationToken(token);
        user.setEmailVerificationTokenExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        user.setPendingEmail(newEmail.toLowerCase());

        userRepository.save(user);
        clearUserCaches(user);

        String verificationUrl = "http://localhost:8080/api/app-users/verify-email?token=" + token;
        mailService.sendEmailChangeVerification(user, verificationUrl);
    }

    public void verifyEmailChange(String token) {
        User user = userRepository.findAll().stream()
            .filter(u -> token.equals(u.getEmailVerificationToken()))
            .findFirst()
            .orElseThrow(() -> new BadRequestAlertException("Invalid token", "appUser", "invalidtoken"));

        if (user.getEmailVerificationTokenExpiry() == null ||
            Instant.now().isAfter(user.getEmailVerificationTokenExpiry())) {
            throw new BadRequestAlertException("Token expired", "appUser", "tokenexpired");
        }

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);

        userRepository.save(user);
        clearUserCaches(user);
    }

    public void deleteUserAccount() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "appUser", "notauthenticated"));

        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        user.setActivated(false);
        userRepository.save(user);
        clearUserCaches(user);
    }

    /**
     * Change admin password via Keycloak.
     * Validates the current password by attempting to obtain a token, then uses Keycloak master admin API to reset password.
     *
     * @param passwordChangeDTO contains current and new passwords
     */
    public void changeAdminPassword(PasswordChangeDTO passwordChangeDTO) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "admin", "notauthenticated"));

        LOG.debug("Request to change password for admin: {}", login);

        // Get the current user's ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new BadRequestAlertException("Invalid authentication", "admin", "invalidauth");
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();
        String userId = jwt.getSubject(); // Keycloak user ID

        String keycloakBaseUrl = keycloakIssuerUri.replace("/realms/jhipster", "");

        // Step 1: Validate current password by attempting to obtain a token with it
        String tokenEndpoint = keycloakBaseUrl + "/realms/jhipster/protocol/openid-connect/token";

        HttpHeaders validateHeaders = new HttpHeaders();
        validateHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String validateBody = "grant_type=password" +
            "&client_id=web_app" +
            "&client_secret=ASoXbE72eEiIpZmvGBObIpN2dNhiyM26" +
            "&username=" + login +
            "&password=" + passwordChangeDTO.getCurrentPassword();

        HttpEntity<String> validateRequest = new HttpEntity<>(validateBody, validateHeaders);

        try {
            // Validate current password
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> validateResponse = restTemplate.postForEntity(
                tokenEndpoint,
                validateRequest,
                Map.class
            );

            if (!validateResponse.getStatusCode().is2xxSuccessful()) {
                throw new BadRequestAlertException("Current password is incorrect", "admin", "invalidcurrentpassword");
            }

            LOG.debug("Current password validated successfully for admin: {}", login);

            // Step 2: Get Keycloak master admin access token
            String masterTokenEndpoint = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";

            HttpHeaders masterHeaders = new HttpHeaders();
            masterHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String masterTokenBody = "grant_type=password" +
                "&client_id=" + applicationProperties.getKeycloak().getAdminClientId() +
                "&username=" + applicationProperties.getKeycloak().getAdminUsername() +
                "&password=" + applicationProperties.getKeycloak().getAdminPassword();

            HttpEntity<String> masterTokenRequest = new HttpEntity<>(masterTokenBody, masterHeaders);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> masterTokenResponse = restTemplate.postForEntity(
                masterTokenEndpoint,
                masterTokenRequest,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> masterTokenData = masterTokenResponse.getBody();
            String masterAccessToken = (String) masterTokenData.get("access_token");

            LOG.debug("Obtained Keycloak master admin token");

            // Step 3: Use Admin API to reset password
            String resetPasswordUrl = keycloakBaseUrl + "/admin/realms/jhipster/users/" + userId + "/reset-password";

            Map<String, Object> passwordResetRequest = new HashMap<>();
            passwordResetRequest.put("type", "password");
            passwordResetRequest.put("temporary", false);
            passwordResetRequest.put("value", passwordChangeDTO.getNewPassword());

            HttpHeaders resetHeaders = new HttpHeaders();
            resetHeaders.setContentType(MediaType.APPLICATION_JSON);
            resetHeaders.setBearerAuth(masterAccessToken);

            HttpEntity<Map<String, Object>> resetRequest = new HttpEntity<>(passwordResetRequest, resetHeaders);

            // PUT request to reset password
            restTemplate.exchange(
                resetPasswordUrl,
                HttpMethod.PUT,
                resetRequest,
                Void.class
            );

            LOG.info("Password successfully changed for admin: {}", login);

        } catch (HttpClientErrorException e) {
            LOG.error("Error changing password for admin: {}, status: {}, error: {}", login, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                // Check if this is from the validation step or the admin API step
                if (e.getMessage().contains("invalid_grant") || e.getResponseBodyAsString().contains("invalid_grant")) {
                    throw new BadRequestAlertException("Current password is incorrect", "admin", "invalidcurrentpassword");
                }
                throw new BadRequestAlertException("Current password is incorrect", "admin", "invalidcurrentpassword");
            }
            throw new BadRequestAlertException("Failed to change password", "admin", "passwordchangefailed");
        } catch (Exception e) {
            LOG.error("Unexpected error changing password for admin: {}", login, e);
            throw new BadRequestAlertException("Failed to change password", "admin", "passwordchangefailed");
        }
    }

    private AppUserDTO mapToDTO(User user) {
        AppUserDTO dto = new AppUserDTO();
        dto.setId(user.getId());
        dto.setLogin(user.getLogin());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setLangKey(user.getLangKey());
        dto.setImageUrl(user.getImageUrl());
        dto.setPhone(user.getPhone());
        dto.setWilaya(user.getWilaya());
        dto.setCity(user.getCity());
        dto.setStreetAddress(user.getStreetAddress());
        dto.setPostalCode(user.getPostalCode());
        dto.setDefaultShippingMethod(user.getDefaultShippingMethod());
        dto.setDefaultShippingProvider(user.getDefaultShippingProvider());
        dto.setActivated(user.isActivated());
        dto.setCreatedDate(user.getCreatedDate());
        return dto;
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evictIfPresent(user.getLogin());
        if (user.getEmail() != null) {
            Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evictIfPresent(user.getEmail());
        }
    }

    /**
     * Get all non-admin users with optional filtering and sorting.
     *
     * @param active Optional filter for active/inactive users (null = all users)
     * @param pageable Pagination and sorting information
     * @return Page of non-admin users
     */
    @Transactional(readOnly = true)
    public Page<AppUserDTO> getAllNonAdminUsers(Boolean active, Pageable pageable) {
        LOG.debug("Request to get all non-admin users with active filter: {}", active);

        Page<User> users;
        if (active != null) {
            users = userRepository.findAllByActivatedAndAuthoritiesNotContaining(active, "ROLE_ADMIN", pageable);
        } else {
            users = userRepository.findAllByAuthoritiesNotContaining("ROLE_ADMIN", pageable);
        }

        return users.map(this::mapToDTO);
    }

    /**
     * Toggle user activation status (admin only).
     *
     * @param userId The ID of the user to toggle
     */
    public void toggleUserActivation(String userId) {
        LOG.debug("Request to toggle activation for user ID: {}", userId);

        User user = userRepository.findOneWithAuthoritiesByLogin(userId)
            .or(() -> userRepository.findById(userId))
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        // Prevent toggling admin users - force loading authorities if not already loaded
        user.getAuthorities().size(); // Force lazy loading
        if (user.getAuthorities().stream().anyMatch(auth -> "ROLE_ADMIN".equals(auth.getName()))) {
            throw new BadRequestAlertException("Cannot toggle admin user activation", "appUser", "cannotmodifyadmin");
        }

        user.setActivated(!user.isActivated());
        userRepository.save(user);
        clearUserCaches(user);

        LOG.info("User '{}' activation toggled to: {}", user.getLogin(), user.isActivated());
    }

    /**
     * Export all non-admin users to Excel format.
     *
     * @return byte array containing the Excel file
     * @throws IOException if there's an error creating the Excel file
     */
    @Transactional(readOnly = true)
    public byte[] exportUsersToExcel() throws IOException {
        LOG.debug("Request to export all non-admin users to Excel");

        // Fetch all non-admin users, sorted by creation date descending
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createdDate"));
        List<User> users = userRepository.findAllByAuthoritiesNotContaining("ROLE_ADMIN", pageable).getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Users");

            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            String[] headers = {
                "ID", "Login", "First Name", "Last Name", "Email", "Phone",
                "Wilaya", "City", "Street Address", "Postal Code",
                "Language", "Shipping Method", "Shipping Provider",
                "Activated", "Image URL", "Created Date"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create date formatter
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());

            // Create cell style for data
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Populate data rows
            int rowNum = 1;
            for (User user : users) {
                Row row = sheet.createRow(rowNum++);

                createCell(row, 0, user.getId(), dataStyle);
                createCell(row, 1, user.getLogin(), dataStyle);
                createCell(row, 2, user.getFirstName(), dataStyle);
                createCell(row, 3, user.getLastName(), dataStyle);
                createCell(row, 4, user.getEmail(), dataStyle);
                createCell(row, 5, user.getPhone(), dataStyle);
                createCell(row, 6, user.getWilaya(), dataStyle);
                createCell(row, 7, user.getCity(), dataStyle);
                createCell(row, 8, user.getStreetAddress(), dataStyle);
                createCell(row, 9, user.getPostalCode(), dataStyle);
                createCell(row, 10, user.getLangKey(), dataStyle);
                createCell(row, 11, user.getDefaultShippingMethod() != null ? user.getDefaultShippingMethod().toString() : "", dataStyle);
                createCell(row, 12, user.getDefaultShippingProvider() != null ? user.getDefaultShippingProvider().toString() : "", dataStyle);
                createCell(row, 13, user.isActivated() ? "Yes" : "No", dataStyle);
                createCell(row, 14, user.getImageUrl(), dataStyle);
                createCell(row, 15, user.getCreatedDate() != null ? dateFormatter.format(user.getCreatedDate()) : "", dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            LOG.info("Successfully exported {} users to Excel", users.size());
            return out.toByteArray();
        }
    }

    /**
     * Helper method to create a cell with value and style.
     */
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }
}
