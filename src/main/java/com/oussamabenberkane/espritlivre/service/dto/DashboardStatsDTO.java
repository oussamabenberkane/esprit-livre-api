package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for overall dashboard statistics including all KPI metrics.
 */
public class DashboardStatsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BestSellingBookDTO bestSellingBook;
    private NewUsersDTO newUsers;
    private Long totalOrders;
    private BigDecimal monthlySales;
    private GrowthMetricsDTO growth;

    public DashboardStatsDTO() {}

    public DashboardStatsDTO(
        BestSellingBookDTO bestSellingBook,
        NewUsersDTO newUsers,
        Long totalOrders,
        BigDecimal monthlySales,
        GrowthMetricsDTO growth
    ) {
        this.bestSellingBook = bestSellingBook;
        this.newUsers = newUsers;
        this.totalOrders = totalOrders;
        this.monthlySales = monthlySales;
        this.growth = growth;
    }

    // Getters and Setters

    public BestSellingBookDTO getBestSellingBook() {
        return bestSellingBook;
    }

    public void setBestSellingBook(BestSellingBookDTO bestSellingBook) {
        this.bestSellingBook = bestSellingBook;
    }

    public NewUsersDTO getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(NewUsersDTO newUsers) {
        this.newUsers = newUsers;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(BigDecimal monthlySales) {
        this.monthlySales = monthlySales;
    }

    public GrowthMetricsDTO getGrowth() {
        return growth;
    }

    public void setGrowth(GrowthMetricsDTO growth) {
        this.growth = growth;
    }

    @Override
    public String toString() {
        return (
            "DashboardStatsDTO{" +
            "bestSellingBook=" +
            bestSellingBook +
            ", newUsers=" +
            newUsers +
            ", totalOrders=" +
            totalOrders +
            ", monthlySales=" +
            monthlySales +
            ", growth=" +
            growth +
            '}'
        );
    }
}
