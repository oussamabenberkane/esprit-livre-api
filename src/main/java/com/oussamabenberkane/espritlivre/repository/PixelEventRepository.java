package com.oussamabenberkane.espritlivre.repository;

import com.oussamabenberkane.espritlivre.domain.PixelEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PixelEventRepository extends JpaRepository<PixelEvent, Long> {

    @Query("SELECT p.eventName, COUNT(p) FROM PixelEvent p WHERE p.firedAt > :cutoff GROUP BY p.eventName")
    List<Object[]> countByEventNameAfter(Instant cutoff);

    @Query("SELECT p.eventName, MAX(p.firedAt) FROM PixelEvent p GROUP BY p.eventName")
    List<Object[]> findLastSeenPerEventName();
}
