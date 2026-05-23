package com.oussamabenberkane.espritlivre.service.dto;

import java.time.Instant;

public class PixelEventSummaryDTO {

    private final String eventName;
    private final long count;
    private final Instant lastSeenAt;

    public PixelEventSummaryDTO(String eventName, long count, Instant lastSeenAt) {
        this.eventName = eventName;
        this.count = count;
        this.lastSeenAt = lastSeenAt;
    }

    public String getEventName() {
        return eventName;
    }

    public long getCount() {
        return count;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
