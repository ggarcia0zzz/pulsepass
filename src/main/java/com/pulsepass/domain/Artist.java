package com.pulsepass.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "artists")
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, unique = true)
    private String stageName;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private boolean active;

    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();

    protected Artist() {

    }

    private Artist(String stageName, String genre, String country) {
        this.stageName = stageName;
        this.genre = genre;
        this.country = country;
        this.active = true;
    }

    public static Artist create(String stageName, String genre, String country) {
        return new Artist(stageName, genre, country);
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getStageName() {
        return stageName;
    }

    public String getGenre() {
        return genre;
    }

    public String getCountry() {
        return country;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Event> getEvents() {
        return events;
    }
}