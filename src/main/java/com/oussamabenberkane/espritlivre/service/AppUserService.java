package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.repository.UserRepository;
import com.oussamabenberkane.espritlivre.security.SecurityUtils;
import com.oussamabenberkane.espritlivre.service.dto.AppUserDTO;
import com.oussamabenberkane.espritlivre.web.rest.errors.BadRequestAlertException;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class AppUserService {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserService.class);

    private final UserRepository userRepository;
    private final MailService mailService;

    public AppUserService(UserRepository userRepository, MailService mailService) {
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    public void completeAppUserRegistration(AppUserDTO appUserDTO) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "appUser", "notauthenticated"));

        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        user.setLastName(appUserDTO.getLastName());
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
        mailService.sendNewUserRegistrationEmailToAdmin(user);
    }

    @Transactional(readOnly = true)
    public AppUserDTO getAppUserProfile(String login) {
        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));
        return mapToDTO(user);
    }

    public void updateAppUserProfile(AppUserDTO appUserDTO) {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "appUser", "notauthenticated"));

        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

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

        String token = RandomStringUtils.randomAlphanumeric(20);
        user.setEmailVerificationToken(token);
        user.setEmailVerificationTokenExpiry(Instant.now().plus(24, ChronoUnit.HOURS));
        user.setPendingEmail(newEmail.toLowerCase());

        userRepository.save(user);

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
    }

    public void deleteUserAccount() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "appUser", "notauthenticated"));

        User user = userRepository.findOneByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("User not found", "appUser", "usernotfound"));

        user.setActivated(false);
        userRepository.save(user);
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
        return dto;
    }
}
