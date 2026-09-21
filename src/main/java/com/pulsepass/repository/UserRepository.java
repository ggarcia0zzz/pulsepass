package com.pulsepass.repository;

import com.pulsepass.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // FR-USR-001 / FR-USR-002: buscar usuario por email ignorando mayúsculas
    Optional<User> findByEmailIgnoreCase(String email);

    // FR-USR-001 / FR-USR-002: buscar usuario por username
    Optional<User> findByUsername(String username);
}