package com.oussamabenberkane.espritlivre.service.dto;

import java.time.LocalDate;

public class SearchTrendPointDTO {

    private final LocalDate date;
    private final long count;

    public SearchTrendPointDTO(LocalDate date, long count) {
        this.date = date;
        this.count = count;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getCount() {
        return count;
    }
}
