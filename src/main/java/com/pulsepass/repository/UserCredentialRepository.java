package com.pulsepass.repository;

import com.pulsepass.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

/** La PK de la credencial es el ID del usuario (user_id). */
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
}
