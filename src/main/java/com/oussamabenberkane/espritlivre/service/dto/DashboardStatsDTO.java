package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;

/**
 * DTO for overall dashboard statistics including all KPI metrics.
 */
public class DashboardStatsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BestSellingBookDTO bestSellingBook;
    private NewUsersDTO newUsers;
    private OrdersBreakdownDTO orders;
    private SalesBreakdownDTO sales;
    private SalesBreakdownDTO grossSales;
    private GrowthMetricsDTO growth;

    public DashboardStatsDTO() {}

    public DashboardStatsDTO(
        BestSellingBookDTO bestSellingBook,
        NewUsersDTO newUsers,
        OrdersBreakdownDTO orders,
        SalesBreakdownDTO sales,
        SalesBreakdownDTO grossSales,
        GrowthMetricsDTO growth
    ) {
        this.bestSellingBook = bestSellingBook;
        this.newUsers = newUsers;
        this.orders = orders;
        this.sales = sales;
        this.grossSales = grossSales;
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

    public OrdersBreakdownDTO getOrders() {
        return orders;
    }

    public void setOrders(OrdersBreakdownDTO orders) {
        this.orders = orders;
    }

    public SalesBreakdownDTO getSales() {
        return sales;
    }

    public void setSales(SalesBreakdownDTO sales) {
        this.sales = sales;
    }

    public SalesBreakdownDTO getGrossSales() {
        return grossSales;
    }

    public void setGrossSales(SalesBreakdownDTO grossSales) {
        this.grossSales = grossSales;
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
            ", orders=" +
            orders +
            ", sales=" +
            sales +
            ", grossSales=" +
            grossSales +
            ", growth=" +
            growth +
            '}'
        );
    }
}
