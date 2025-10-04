package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.service.UserService;
import com.oussamabenberkane.espritlivre.service.dto.AdminUserDTO;
import com.oussamabenberkane.espritlivre.service.dto.UserProfileDTO;
import jakarta.validation.Valid;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private static class AccountResourceException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private AccountResourceException(String message) {
            super(message);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AccountResource.class);

    private final UserService userService;

    public AccountResource(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @param principal the current user; resolves to {@code null} if not authenticated.
     * @return the current user.
     * @throws AccountResourceException {@code 500 (Internal Server Error)} if the user couldn't be returned.
     */
    @GetMapping("/account")
    public AdminUserDTO getAccount(Principal principal) {
        if (principal instanceof AbstractAuthenticationToken) {
            return userService.getUserFromAuthentication((AbstractAuthenticationToken) principal);
        } else {
            throw new AccountResourceException("User could not be found");
        }
    }

    /**
     * {@code GET  /authenticate} : check if the user is authenticated.
     *
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)},
     * or with status {@code 401 (Unauthorized)} if not authenticated.
     */
    @GetMapping("/authenticate")
    public ResponseEntity<Void> isAuthenticated(Principal principal) {
        LOG.debug("REST request to check if the current user is authenticated");
        return ResponseEntity.status(principal == null ? HttpStatus.UNAUTHORIZED : HttpStatus.NO_CONTENT).build();
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userProfileDTO the current user information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     * @throws AccountResourceException {@code 400 (Bad Request)} if the email is already in use.
     */
    @PostMapping("/account")
    public ResponseEntity<Void> saveAccount(@Valid @RequestBody UserProfileDTO userProfileDTO) {
        LOG.debug("REST request to update user profile : {}", userProfileDTO);
        userService.updateUser(
            userProfileDTO.getFirstName(),
            userProfileDTO.getLastName(),
            userProfileDTO.getEmail(),
            userProfileDTO.getLangKey(),
            userProfileDTO.getImageUrl()
        );
        return ResponseEntity.ok().build();
    }
}
