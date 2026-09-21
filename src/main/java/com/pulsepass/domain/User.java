package com.pulsepass.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean active;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, optional = true)
    private UserProfile profile;

    @OneToMany(mappedBy = "user")
    private List<Ticket> tickets = new ArrayList<>();

    protected User() {

    }

    private User(String username, String email) {
        this.username = username;
        this.email = email;
        this.active = true;
    }

    public static User create(String username, String email) {
        return new User(username, email);
    }

    public void deactivate() {
        this.active = false;
    }

    public void assignProfile(UserProfile profile) {
        this.profile = profile;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }
}