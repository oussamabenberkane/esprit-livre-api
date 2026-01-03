package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;

/**
 * DTO for all growth metrics across different KPIs.
 */
public class GrowthMetricsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private GrowthMetricDTO bestSellingBook;
    private GrowthMetricDTO newUsers;
    private GrowthMetricDTO orders;
    private GrowthMetricDTO sales;

    public GrowthMetricsDTO() {}

    public GrowthMetricsDTO(
        GrowthMetricDTO bestSellingBook,
        GrowthMetricDTO newUsers,
        GrowthMetricDTO orders,
        GrowthMetricDTO sales
    ) {
        this.bestSellingBook = bestSellingBook;
        this.newUsers = newUsers;
        this.orders = orders;
        this.sales = sales;
    }

    // Getters and Setters

    public GrowthMetricDTO getBestSellingBook() {
        return bestSellingBook;
    }

    public void setBestSellingBook(GrowthMetricDTO bestSellingBook) {
        this.bestSellingBook = bestSellingBook;
    }

    public GrowthMetricDTO getNewUsers() {
        return newUsers;
    }

    public void setNewUsers(GrowthMetricDTO newUsers) {
        this.newUsers = newUsers;
    }

    public GrowthMetricDTO getOrders() {
        return orders;
    }

    public void setOrders(GrowthMetricDTO orders) {
        this.orders = orders;
    }

    public GrowthMetricDTO getSales() {
        return sales;
    }

    public void setSales(GrowthMetricDTO sales) {
        this.sales = sales;
    }

    @Override
    public String toString() {
        return (
            "GrowthMetricsDTO{" +
            "bestSellingBook=" +
            bestSellingBook +
            ", newUsers=" +
            newUsers +
            ", orders=" +
            orders +
            ", sales=" +
            sales +
            '}'
        );
    }
}
