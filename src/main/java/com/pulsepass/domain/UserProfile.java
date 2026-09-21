package com.pulsepass.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phone;

    @Column
    private String city;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    protected UserProfile() {

    }

    private UserProfile(String firstName, String lastName, String phone, String city,
                        LocalDate birthDate, User user) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.city = city;
        this.birthDate = birthDate;
        this.user = user;
    }

    public static UserProfile create(String firstName, String lastName, String phone, String city,
                                     LocalDate birthDate, User user) {
        return new UserProfile(firstName, lastName, phone, city, birthDate, user);
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public User getUser() {
        return user;
    }
}