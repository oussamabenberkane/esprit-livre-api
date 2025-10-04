package com.oussamabenberkane.espritlivre.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for changing email.
 */
public class EmailChangeDTO {

    @NotNull
    @Email
    @Size(min = 5, max = 254)
    private String newEmail;

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }
}
