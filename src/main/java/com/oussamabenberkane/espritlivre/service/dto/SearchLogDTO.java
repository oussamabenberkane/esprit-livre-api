package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.SearchLog;
import java.time.Instant;

public class SearchLogDTO {

    private final Long id;
    private final String searchTerm;
    private final Integer resultsCount;
    private final String userLogin;
    private final String visitorId;
    private final Instant searchedAt;

    public SearchLogDTO(SearchLog searchLog) {
        this.id = searchLog.getId();
        this.searchTerm = searchLog.getSearchTerm();
        this.resultsCount = searchLog.getResultsCount();
        this.userLogin = searchLog.getUserLogin();
        this.visitorId = searchLog.getVisitorId();
        this.searchedAt = searchLog.getSearchedAt();
    }

    public Long getId() {
        return id;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public Integer getResultsCount() {
        return resultsCount;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public Instant getSearchedAt() {
        return searchedAt;
    }
}
