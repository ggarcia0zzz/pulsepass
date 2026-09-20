package com.pulsepass.domain;

import com.pulsepass.domain.enums.EventCategory;
import com.pulsepass.domain.enums.EventStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_code", nullable = false, unique = true)
    private String eventCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "streaming_url", length = 500)
    private String streamingUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToMany
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    protected Event() {

    }

    private Event(String eventCode, String name, EventCategory category, LocalDateTime eventDate, Venue venue) {
        this.eventCode = eventCode;
        this.name = name;
        this.category = category;
        this.eventDate = eventDate;
        this.venue = venue;
        this.status = EventStatus.DRAFT;
    }

    public static Event create(String eventCode, String name, EventCategory category,
                               LocalDateTime eventDate, Venue venue) {
        return new Event(eventCode, name, category, eventDate, venue);
    }

    public void publish() {
        this.status = EventStatus.PUBLISHED;
    }

    public void cancel() {
        this.status = EventStatus.CANCELLED;
    }

    public void addArtist(Artist artist) {
        this.artists.add(artist);
        artist.getEvents().add(this);
    }

    public void setStreamingUrl(String streamingUrl) {
        this.streamingUrl = streamingUrl;
    }

    public Long getId() {
        return id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getName() {
        return name;
    }

    public EventCategory getCategory() {
        return category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public String getStreamingUrl() {
        return streamingUrl;
    }

    public Venue getVenue() {
        return venue;
    }

    public Set<Artist> getArtists() {
        return artists;
    }
}
