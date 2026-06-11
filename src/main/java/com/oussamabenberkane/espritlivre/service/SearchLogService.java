package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.domain.SearchLog;
import com.oussamabenberkane.espritlivre.repository.SearchLogRepository;
import com.oussamabenberkane.espritlivre.service.dto.SearchLogDTO;
import com.oussamabenberkane.espritlivre.service.dto.SearchLogStatsDTO;
import com.oussamabenberkane.espritlivre.service.dto.SearchTermStatDTO;
import com.oussamabenberkane.espritlivre.service.dto.SearchTrendPointDTO;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Persists committed user searches for analysis (top terms, zero-result terms, trends).
 */
@Service
public class SearchLogService {

    private static final Logger LOG = LoggerFactory.getLogger(SearchLogService.class);

    private static final Duration DEDUP_WINDOW = Duration.ofMinutes(30);
    private static final int MAX_TERM_LENGTH = 255;
    private static final int MAX_VISITOR_ID_LENGTH = 64;

    private final SearchLogRepository searchLogRepository;

    public SearchLogService(SearchLogRepository searchLogRepository) {
        this.searchLogRepository = searchLogRepository;
    }

    /**
     * Persist a committed search. Caller must resolve the user login beforehand
     * (the security context does not propagate to the async executor).
     * Never throws: logging must not break search itself.
     */
    @Async
    @Transactional
    public void logSearch(String rawTerm, long resultsCount, String visitorId, String userLogin) {
        try {
            if (!StringUtils.hasText(rawTerm)) {
                return;
            }
            String searchTerm = truncate(rawTerm.trim(), MAX_TERM_LENGTH);
            String normalizedTerm = normalize(searchTerm);
            String safeVisitorId = StringUtils.hasText(visitorId) ? truncate(visitorId.trim(), MAX_VISITOR_ID_LENGTH) : null;

            if (isDuplicate(normalizedTerm, safeVisitorId, userLogin)) {
                return;
            }

            int count = (int) Math.min(resultsCount, Integer.MAX_VALUE);
            searchLogRepository.save(new SearchLog(searchTerm, normalizedTerm, count, userLogin, safeVisitorId, Instant.now()));
        } catch (Exception e) {
            LOG.warn("Failed to persist search log for term '{}'", rawTerm, e);
        }
    }

    /**
     * Same (visitor or user, term) pair within the dedup window: SPA refetches
     * (tab/language switches) re-hit the books endpoint with an unchanged query.
     */
    private boolean isDuplicate(String normalizedTerm, String visitorId, String userLogin) {
        Instant windowStart = Instant.now().minus(DEDUP_WINDOW);
        if (visitorId != null) {
            return searchLogRepository.existsByVisitorIdAndNormalizedTermAndSearchedAtAfter(visitorId, normalizedTerm, windowStart);
        }
        if (userLogin != null) {
            return searchLogRepository.existsByUserLoginAndNormalizedTermAndSearchedAtAfter(userLogin, normalizedTerm, windowStart);
        }
        return false;
    }

    private static String normalize(String term) {
        return term.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @Transactional(readOnly = true)
    public SearchLogStatsDTO getStats(int days) {
        Instant since = since(days);
        long total = searchLogRepository.countBySearchedAtGreaterThanEqual(since);
        long uniqueTerms = searchLogRepository.countDistinctTermsSince(since);
        long zeroResults = searchLogRepository.countBySearchedAtGreaterThanEqualAndResultsCount(since, 0);
        double zeroResultRate = total == 0 ? 0.0 : (double) zeroResults / total;
        return new SearchLogStatsDTO(total, uniqueTerms, zeroResults, zeroResultRate);
    }

    @Transactional(readOnly = true)
    public List<SearchTermStatDTO> getTopTerms(int days, int limit) {
        return searchLogRepository
            .findTopTermsSince(since(days), PageRequest.of(0, limit))
            .stream()
            .map(SearchLogService::toTermStatDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchTermStatDTO> getTopZeroResultTerms(int days, int limit) {
        return searchLogRepository
            .findTopZeroResultTermsSince(since(days), PageRequest.of(0, limit))
            .stream()
            .map(SearchLogService::toTermStatDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SearchTrendPointDTO> getDailyTrend(int days) {
        return searchLogRepository
            .findDailyTrendSince(since(days))
            .stream()
            .map(p -> new SearchTrendPointDTO(p.getDay().toLocalDate(), p.getSearchCount()))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<SearchLogDTO> getRecentSearches(Pageable pageable) {
        return searchLogRepository.findAllByOrderBySearchedAtDesc(pageable).map(SearchLogDTO::new);
    }

    private static Instant since(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    private static SearchTermStatDTO toTermStatDTO(SearchLogRepository.SearchTermStatProjection p) {
        return new SearchTermStatDTO(p.getTerm(), p.getSearchCount(), p.getAvgResults(), p.getLastSearchedAt());
    }
}
