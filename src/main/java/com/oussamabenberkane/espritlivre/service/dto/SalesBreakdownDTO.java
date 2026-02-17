package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for sales statistics with breakdown by time period.
 */
public class SalesBreakdownDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal total;
    private BigDecimal today;
    private BigDecimal thisWeek;
    private BigDecimal thisMonth;

    public SalesBreakdownDTO() {}

    public SalesBreakdownDTO(BigDecimal total, BigDecimal today, BigDecimal thisWeek, BigDecimal thisMonth) {
        this.total = total;
        this.today = today;
        this.thisWeek = thisWeek;
        this.thisMonth = thisMonth;
    }

    // Getters and Setters

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getToday() {
        return today;
    }

    public void setToday(BigDecimal today) {
        this.today = today;
    }

    public BigDecimal getThisWeek() {
        return thisWeek;
    }

    public void setThisWeek(BigDecimal thisWeek) {
        this.thisWeek = thisWeek;
    }

    public BigDecimal getThisMonth() {
        return thisMonth;
    }

    public void setThisMonth(BigDecimal thisMonth) {
        this.thisMonth = thisMonth;
    }

    @Override
    public String toString() {
        return (
            "SalesBreakdownDTO{" + "total=" + total + ", today=" + today + ", thisWeek=" + thisWeek + ", thisMonth=" + thisMonth + '}'
        );
    }
}
