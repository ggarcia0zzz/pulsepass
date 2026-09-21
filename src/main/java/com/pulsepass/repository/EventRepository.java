package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // FR-EVT-001 / AC-002: recuperar un evento por su código de negocio
    Optional<Event> findByEventCode(String eventCode);

    // FR-EVT-005 / UC-06: eventos por estado ordenados por fecha ascendente
    // (se usa con EventStatus.PUBLISHED para la cartelera)
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // FR-VEN-004: eventos de un venue navegando la relación (venue.code)
    List<Event> findByVenue_Code(String venueCode);

    // FR-SRC-001 / FR-ART-004 / AC-007: eventos de un artista, sin duplicados
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.artists a
            WHERE a.stageName = :stageName
            """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    // FR-SRC-002: eventos de una ciudad en los que participa un artista
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE v.city = :city
              AND a.stageName = :stageName
            """)
    List<Event> findByCityAndArtistStageName(@Param("city") String city,
                                             @Param("stageName") String stageName);

    // FR-SRC-003 / UC-09: eventos recomendados
    // publicados, posteriores a una fecha, en una ciudad y con un artista cuyo
    // nombre contenga el texto (case-insensitive), sin duplicados y por fecha
    @Query("""
            SELECT DISTINCT e
            FROM Event e
            JOIN e.venue v
            JOIN e.artists a
            WHERE e.status = com.pulsepass.domain.enums.EventStatus.PUBLISHED
              AND e.eventDate > :after
              AND v.city = :city
              AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
            ORDER BY e.eventDate ASC
            """)
    List<Event> findRecommended(@Param("after") LocalDateTime after,
                                @Param("city") String city,
                                @Param("artistText") String artistText);
}