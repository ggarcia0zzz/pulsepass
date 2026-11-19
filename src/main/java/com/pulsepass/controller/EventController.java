package com.pulsepass.controller;

import com.pulsepass.dto.EventCreateRequest;
import com.pulsepass.dto.EventResponse;
import com.pulsepass.security.AuthenticatedUser;
import com.pulsepass.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;

    public EventController(EventService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventCreateRequest request) { return service.create(request); }

    @GetMapping("/published")
    public List<EventResponse> published() { return service.listPublished(); }

    @GetMapping("/by-artist")
    public List<EventResponse> byArtist(@RequestParam String stageName, @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.findByArtist(stageName, actor);
    }

    @GetMapping("/{eventCode}")
    public EventResponse byCode(@PathVariable String eventCode, @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.getByCode(eventCode, actor);
    }

    @PatchMapping("/{eventCode}/publish")
    public EventResponse publish(@PathVariable String eventCode) { return service.publish(eventCode); }

    @PostMapping("/{eventCode}/artists/{artistId}")
    public EventResponse addArtist(@PathVariable String eventCode, @PathVariable Long artistId) {
        return service.addArtist(eventCode, artistId);
    }
}
