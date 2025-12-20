package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;

/**
 * DTO for profile update response, including counts of linked guest orders and updated existing orders.
 */
public class ProfileUpdateResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer linkedOrdersCount;
    private Integer updatedOrdersCount;

    public ProfileUpdateResponseDTO() {
    }

    public ProfileUpdateResponseDTO(Integer linkedOrdersCount, Integer updatedOrdersCount) {
        this.linkedOrdersCount = linkedOrdersCount;
        this.updatedOrdersCount = updatedOrdersCount;
    }

    public Integer getLinkedOrdersCount() {
        return linkedOrdersCount;
    }

    public void setLinkedOrdersCount(Integer linkedOrdersCount) {
        this.linkedOrdersCount = linkedOrdersCount;
    }

    public Integer getUpdatedOrdersCount() {
        return updatedOrdersCount;
    }

    public void setUpdatedOrdersCount(Integer updatedOrdersCount) {
        this.updatedOrdersCount = updatedOrdersCount;
    }

    @Override
    public String toString() {
        return "ProfileUpdateResponseDTO{" +
            "linkedOrdersCount=" + linkedOrdersCount +
            ", updatedOrdersCount=" + updatedOrdersCount +
            '}';
    }
}
