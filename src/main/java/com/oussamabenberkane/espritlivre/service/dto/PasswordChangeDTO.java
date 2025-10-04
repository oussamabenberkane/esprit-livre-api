package com.oussamabenberkane.espritlivre.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for changing password.
 */
public class PasswordChangeDTO {

    @NotNull
    @Size(min = 4, max = 100)
    private String currentPassword;

    @NotNull
    @Size(min = 4, max = 100)
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
