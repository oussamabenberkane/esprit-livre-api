package com.oussamabenberkane.espritlivre.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "search_log")
public class SearchLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "searchLogSequenceGenerator")
    @SequenceGenerator(name = "searchLogSequenceGenerator", sequenceName = "search_log_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "search_term", nullable = false, length = 255)
    private String searchTerm;

    @Column(name = "normalized_term", nullable = false, length = 255)
    private String normalizedTerm;

    @Column(name = "results_count", nullable = false)
    private Integer resultsCount;

    @Column(name = "user_login", length = 100)
    private String userLogin;

    @Column(name = "visitor_id", length = 64)
    private String visitorId;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    public SearchLog() {}

    public SearchLog(String searchTerm, String normalizedTerm, Integer resultsCount, String userLogin, String visitorId, Instant searchedAt) {
        this.searchTerm = searchTerm;
        this.normalizedTerm = normalizedTerm;
        this.resultsCount = resultsCount;
        this.userLogin = userLogin;
        this.visitorId = visitorId;
        this.searchedAt = searchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public String getNormalizedTerm() {
        return normalizedTerm;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchLog)) return false;
        return id != null && id.equals(((SearchLog) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SearchLog{id=" + id + ", searchTerm='" + searchTerm + "', resultsCount=" + resultsCount + ", searchedAt=" + searchedAt + "}";
    }
}
