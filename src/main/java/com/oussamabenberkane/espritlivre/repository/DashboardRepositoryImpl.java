package com.oussamabenberkane.espritlivre.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Implementation of custom dashboard queries.
 * Uses native SQL for optimized performance on aggregated data.
 */
@Repository
public class DashboardRepositoryImpl implements DashboardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public BestSellingBookProjection findBestSellingBook(ZonedDateTime startDate, ZonedDateTime endDate) {
        String sql =
            """
            SELECT
                b.id as bookId,
                b.title as bookTitle,
                COALESCE(SUM(oi.quantity), 0) as totalQuantity,
                b.price as bookPrice
            FROM book b
            LEFT JOIN order_item oi ON b.id = oi.book_id
            LEFT JOIN jhi_order o ON oi.order_id = o.id
            WHERE b.active = true
              AND b.deleted_at IS NULL
              AND (o.id IS NULL OR (
                o.deleted_at IS NULL
                AND o.created_at >= :startDate
                AND o.created_at < :endDate
              ))
            GROUP BY b.id, b.title, b.price
            ORDER BY totalQuantity DESC
            LIMIT 1
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        }

        Object[] row = results.get(0);
        return new BestSellingBookProjection() {
            @Override
            public Long getBookId() {
                return row[0] != null ? ((Number) row[0]).longValue() : null;
            }

            @Override
            public String getBookTitle() {
                return (String) row[1];
            }

            @Override
            public Long getTotalQuantity() {
                return row[2] != null ? ((Number) row[2]).longValue() : 0L;
            }

            @Override
            public BigDecimal getBookPrice() {
                return row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            }
        };
    }

    @Override
    public Long countOrdersByDateRange(ZonedDateTime startDate, ZonedDateTime endDate) {
        String sql =
            """
            SELECT COUNT(*)
            FROM jhi_order o
            WHERE o.deleted_at IS NULL
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        Number result = (Number) query.getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    @Override
    public BigDecimal sumSalesByDateRange(ZonedDateTime startDate, ZonedDateTime endDate) {
        String sql =
            """
            SELECT COALESCE(SUM(o.total_amount), 0)
            FROM jhi_order o
            WHERE o.deleted_at IS NULL
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        Number result = (Number) query.getSingleResult();
        return result != null ? new BigDecimal(result.toString()) : BigDecimal.ZERO;
    }

    @Override
    public Long countUsersByCreatedAtRange(ZonedDateTime startDate, ZonedDateTime endDate) {
        String sql =
            """
            SELECT COUNT(*)
            FROM jhi_user u
            WHERE u.created_date >= :startDate
              AND u.created_date < :endDate
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        Number result = (Number) query.getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    @Override
    public List<SalesDataProjection> getHourlySales(ZonedDateTime startOfDay, ZonedDateTime endOfDay) {
        String sql =
            """
            SELECT
                TO_CHAR(o.created_at, 'HH24') as timePeriod,
                COALESCE(SUM(o.total_amount), 0) as totalSales
            FROM jhi_order o
            WHERE o.deleted_at IS NULL
              AND o.created_at >= :startOfDay
              AND o.created_at < :endOfDay
            GROUP BY TO_CHAR(o.created_at, 'HH24')
            ORDER BY timePeriod
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startOfDay", startOfDay);
        query.setParameter("endOfDay", endOfDay);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row ->
                new SalesDataProjection() {
                    @Override
                    public String getTimePeriod() {
                        return (String) row[0];
                    }

                    @Override
                    public BigDecimal getTotalSales() {
                        return row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    }
                }
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<SalesDataProjection> getDailySales(ZonedDateTime startOfWeek, ZonedDateTime endOfWeek) {
        String sql =
            """
            SELECT
                TO_CHAR(o.created_at, 'ID') as timePeriod,
                COALESCE(SUM(o.total_amount), 0) as totalSales
            FROM jhi_order o
            WHERE o.deleted_at IS NULL
              AND o.created_at >= :startOfWeek
              AND o.created_at < :endOfWeek
            GROUP BY TO_CHAR(o.created_at, 'ID')
            ORDER BY timePeriod
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startOfWeek", startOfWeek);
        query.setParameter("endOfWeek", endOfWeek);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row ->
                new SalesDataProjection() {
                    @Override
                    public String getTimePeriod() {
                        return (String) row[0];
                    }

                    @Override
                    public BigDecimal getTotalSales() {
                        return row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    }
                }
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<SalesDataProjection> getWeeklySales(ZonedDateTime startOfMonth, ZonedDateTime endOfMonth) {
        String sql =
            """
            SELECT
                TO_CHAR(o.created_at, 'IW') as timePeriod,
                COALESCE(SUM(o.total_amount), 0) as totalSales
            FROM jhi_order o
            WHERE o.deleted_at IS NULL
              AND o.created_at >= :startOfMonth
              AND o.created_at < :endOfMonth
            GROUP BY TO_CHAR(o.created_at, 'IW')
            ORDER BY timePeriod
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startOfMonth", startOfMonth);
        query.setParameter("endOfMonth", endOfMonth);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row ->
                new SalesDataProjection() {
                    @Override
                    public String getTimePeriod() {
                        return (String) row[0];
                    }

                    @Override
                    public BigDecimal getTotalSales() {
                        return row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    }
                }
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<SalesDataProjection> getMonthlySales(ZonedDateTime startOfYear, ZonedDateTime endOfYear) {
        String sql =
            """
            SELECT
                TO_CHAR(o.created_at, 'MM') as timePeriod,
                COALESCE(SUM(o.total_amount), 0) as totalSales
            FROM jhi_order o
            WHERE o.deleted_at IS NULL
              AND o.created_at >= :startOfYear
              AND o.created_at < :endOfYear
            GROUP BY TO_CHAR(o.created_at, 'MM')
            ORDER BY timePeriod
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startOfYear", startOfYear);
        query.setParameter("endOfYear", endOfYear);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row ->
                new SalesDataProjection() {
                    @Override
                    public String getTimePeriod() {
                        return (String) row[0];
                    }

                    @Override
                    public BigDecimal getTotalSales() {
                        return row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    }
                }
            )
            .collect(Collectors.toList());
    }
}
