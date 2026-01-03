package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for a single data point in the sales chart time series.
 */
public class SalesDataPointDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private BigDecimal sales;

    public SalesDataPointDTO() {}

    public SalesDataPointDTO(String name, BigDecimal sales) {
        this.name = name;
        this.sales = sales;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getSales() {
        return sales;
    }

    public void setSales(BigDecimal sales) {
        this.sales = sales;
    }

    @Override
    public String toString() {
        return "SalesDataPointDTO{" + "name='" + name + '\'' + ", sales=" + sales + '}';
    }
}
