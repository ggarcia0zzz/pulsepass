package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    // FR-ART-001 / FR-ART-002: recuperar un artista por su nombre artístico
    Optional<Artist> findByStageName(String stageName);
}