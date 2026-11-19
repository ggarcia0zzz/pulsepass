package com.pulsepass.dto;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.enums.UserRole;

import java.time.LocalDate;
import java.util.List;

/** Nunca incluye password ni hash. STAFF y ADMIN tambien traen el rol USER. */
public record UserResponse(
        Long id, String username, String email, String firstName, String lastName,
        String phone, String city, LocalDate birthDate, boolean active, List<UserRole> roles) {

    public static UserResponse from(User u) {
        UserProfile p = u.getProfile();
        List<UserRole> roles = u.getRole() == UserRole.USER ? List.of(UserRole.USER) : List.of(UserRole.USER, u.getRole());
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(),
                p == null ? null : p.getFirstName(), p == null ? null : p.getLastName(),
                p == null ? null : p.getPhone(), p == null ? null : p.getCity(),
                p == null ? null : p.getBirthDate(), u.isActive(), roles);
    }
}
