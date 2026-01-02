package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.repository.UserRepository;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.dto.AppUserDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class AppUserService {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserService.class);

    private final UserRepository userRepository;
    private final MailService mailService;
    private final CacheManager cacheManager;
    private final OrderService orderService;

    public AppUserService(UserRepository userRepository, MailService mailService, CacheManager cacheManager, OrderService orderService) {
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.cacheManager = cacheManager;
        this.orderService = orderService;
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

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        // Prevent toggling admin users
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
