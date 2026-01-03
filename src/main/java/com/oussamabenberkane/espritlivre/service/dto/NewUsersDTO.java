package com.oussamabenberkane.espritlivre.service.dto;

import java.io.Serializable;

/**
 * DTO for new users statistics with breakdown by time period.
 */
public class NewUsersDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long total;
    private Long today;
    private Long thisWeek;
    private Long thisMonth;

    public NewUsersDTO() {}

    public NewUsersDTO(Long total, Long today, Long thisWeek, Long thisMonth) {
        this.total = total;
        this.today = today;
        this.thisWeek = thisWeek;
        this.thisMonth = thisMonth;
    }

    // Getters and Setters

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getToday() {
        return today;
    }

    public void setToday(Long today) {
        this.today = today;
    }

    public Long getThisWeek() {
        return thisWeek;
    }

    public void setThisWeek(Long thisWeek) {
        this.thisWeek = thisWeek;
    }

    public Long getThisMonth() {
        return thisMonth;
    }

    public void setThisMonth(Long thisMonth) {
        this.thisMonth = thisMonth;
    }

    @Override
    public String toString() {
        return (
            "NewUsersDTO{" + "total=" + total + ", today=" + today + ", thisWeek=" + thisWeek + ", thisMonth=" + thisMonth + '}'
        );
    }
}
