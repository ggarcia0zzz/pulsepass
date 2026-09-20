package com.pulsepass.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    protected UserProfile() {

    }

    private UserProfile(String fullName, String phone, LocalDate birthDate, User user) {
        this.fullName = fullName;
        this.phone = phone;
        this.birthDate = birthDate;
        this.user = user;
    }

    public static UserProfile create(String fullName, String phone, LocalDate birthDate, User user) {
        return new UserProfile(fullName, phone, birthDate, user);
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public User getUser() {
        return user;
    }

}
