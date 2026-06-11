package com.oussamabenberkane.espritlivre.service.dto;

import java.time.Instant;

public class SearchTermStatDTO {

    private final String term;
    private final long count;
    private final Double avgResults;
    private final Instant lastSearchedAt;

    public SearchTermStatDTO(String term, long count, Double avgResults, Instant lastSearchedAt) {
        this.term = term;
        this.count = count;
        this.avgResults = avgResults;
        this.lastSearchedAt = lastSearchedAt;
    }

    public String getTerm() {
        return term;
    }

    public long getCount() {
        return count;
    }

    public Double getAvgResults() {
        return avgResults;
    }

    public Instant getLastSearchedAt() {
        return lastSearchedAt;
    }
}
