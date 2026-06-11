package com.oussamabenberkane.espritlivre.service.dto;

public class SearchLogStatsDTO {

    private final long totalSearches;
    private final long uniqueTerms;
    private final long zeroResultSearches;
    private final double zeroResultRate;

    public SearchLogStatsDTO(long totalSearches, long uniqueTerms, long zeroResultSearches, double zeroResultRate) {
        this.totalSearches = totalSearches;
        this.uniqueTerms = uniqueTerms;
        this.zeroResultSearches = zeroResultSearches;
        this.zeroResultRate = zeroResultRate;
    }

    public long getTotalSearches() {
        return totalSearches;
    }

    public long getUniqueTerms() {
        return uniqueTerms;
    }

    public long getZeroResultSearches() {
        return zeroResultSearches;
    }

    public double getZeroResultRate() {
        return zeroResultRate;
    }
}
