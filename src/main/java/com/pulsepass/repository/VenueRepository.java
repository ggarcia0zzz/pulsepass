package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    // FR-VEN-001: recuperar un venue por su código de negocio
    Optional<Venue> findByCode(String code);

    List<Venue> findByActiveTrue();
}
