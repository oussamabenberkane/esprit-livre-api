package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;

/**
 * DTO for individual growth metric with percentage and trend direction.
 */
public class GrowthMetricDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Double value;
    private Boolean isPositive;

    public GrowthMetricDTO() {}

    public GrowthMetricDTO(Double value, Boolean isPositive) {
        this.value = value;
        this.isPositive = isPositive;
    }

    // Getters and Setters

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Boolean getIsPositive() {
        return isPositive;
    }

    public void setIsPositive(Boolean isPositive) {
        this.isPositive = isPositive;
    }

    @Override
    public String toString() {
        return "GrowthMetricDTO{" + "value=" + value + ", isPositive=" + isPositive + '}';
    }
}
