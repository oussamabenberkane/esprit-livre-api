package com.oussamabenberkane.espritlivre.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "pixel_event")
public class PixelEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pixelEventSequenceGenerator")
    @SequenceGenerator(name = "pixelEventSequenceGenerator", sequenceName = "pixel_event_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    public PixelEvent() {}

    public PixelEvent(String eventName, Instant firedAt) {
        this.eventName = eventName;
        this.firedAt = firedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventName() {
        return eventName;
    }

    public Instant getFiredAt() {
        return firedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PixelEvent)) return false;
        return id != null && id.equals(((PixelEvent) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PixelEvent{id=" + id + ", eventName='" + eventName + "', firedAt=" + firedAt + "}";
    }
}
