package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for image migration status tracking.
 */
public class MigrationStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean inProgress;
    private int totalImages;
    private int processedImages;
    private int successCount;
    private int errorCount;
    private List<String> errors;

    public MigrationStatusDTO() {
        this.errors = new ArrayList<>();
    }

    public MigrationStatusDTO(boolean inProgress, int totalImages, int processedImages, int successCount, int errorCount, List<String> errors) {
        this.inProgress = inProgress;
        this.totalImages = totalImages;
        this.processedImages = processedImages;
        this.successCount = successCount;
        this.errorCount = errorCount;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public void setTotalImages(int totalImages) {
        this.totalImages = totalImages;
    }

    public int getProcessedImages() {
        return processedImages;
    }

    public void setProcessedImages(int processedImages) {
        this.processedImages = processedImages;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }

    @Override
    public String toString() {
        return "MigrationStatusDTO{" +
            "inProgress=" + inProgress +
            ", totalImages=" + totalImages +
            ", processedImages=" + processedImages +
            ", successCount=" + successCount +
            ", errorCount=" + errorCount +
            ", errors=" + errors +
            '}';
    }
}
