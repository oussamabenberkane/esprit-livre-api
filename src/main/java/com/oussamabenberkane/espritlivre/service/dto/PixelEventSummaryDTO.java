package com.oussamabenberkane.espritlivre.service.dto;

import java.time.Instant;

public class PixelEventSummaryDTO {

    private final String eventName;
    private final long count24h;
    private final Instant lastSeenAt;

    public PixelEventSummaryDTO(String eventName, long count24h, Instant lastSeenAt) {
        this.eventName = eventName;
        this.count24h = count24h;
        this.lastSeenAt = lastSeenAt;
    }

    public String getEventName() {
        return eventName;
    }

    public long getCount24h() {
        return count24h;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
