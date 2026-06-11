package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.SearchLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    boolean existsByVisitorIdAndNormalizedTermAndSearchedAtAfter(String visitorId, String normalizedTerm, Instant after);

    boolean existsByUserLoginAndNormalizedTermAndSearchedAtAfter(String userLogin, String normalizedTerm, Instant after);

    long countBySearchedAtGreaterThanEqual(Instant since);

    long countBySearchedAtGreaterThanEqualAndResultsCount(Instant since, Integer resultsCount);

    @Query("select count(distinct s.normalizedTerm) from SearchLog s where s.searchedAt >= :since")
    long countDistinctTermsSince(@Param("since") Instant since);

    @Query(
        "select s.normalizedTerm as term, count(s) as searchCount, avg(s.resultsCount) as avgResults, max(s.searchedAt) as lastSearchedAt " +
        "from SearchLog s where s.searchedAt >= :since " +
        "group by s.normalizedTerm order by count(s) desc, max(s.searchedAt) desc"
    )
    List<SearchTermStatProjection> findTopTermsSince(@Param("since") Instant since, Pageable pageable);

    @Query(
        "select s.normalizedTerm as term, count(s) as searchCount, avg(s.resultsCount) as avgResults, max(s.searchedAt) as lastSearchedAt " +
        "from SearchLog s where s.searchedAt >= :since and s.resultsCount = 0 " +
        "group by s.normalizedTerm order by count(s) desc, max(s.searchedAt) desc"
    )
    List<SearchTermStatProjection> findTopZeroResultTermsSince(@Param("since") Instant since, Pageable pageable);

    @Query(
        value = "select cast(searched_at as date) as day, count(*) as searchCount " +
                "from search_log where searched_at >= :since " +
                "group by cast(searched_at as date) order by day",
        nativeQuery = true
    )
    List<SearchTrendProjection> findDailyTrendSince(@Param("since") Instant since);

    Page<SearchLog> findAllByOrderBySearchedAtDesc(Pageable pageable);

    interface SearchTermStatProjection {
        String getTerm();
        long getSearchCount();
        Double getAvgResults();
        Instant getLastSearchedAt();
    }

    interface SearchTrendProjection {
        java.sql.Date getDay();
        long getSearchCount();
    }
}
