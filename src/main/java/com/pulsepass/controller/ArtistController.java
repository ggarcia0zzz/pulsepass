package com.pulsepass.controller;

import com.pulsepass.dto.ArtistResponse;
import com.pulsepass.service.ArtistService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {
    private final ArtistService service;

    public ArtistController(ArtistService service) { this.service = service; }

    @GetMapping("/active")
    public List<ArtistResponse> active() { return service.listActive(); }

    @GetMapping("/by-stage-name")
    public ArtistResponse byStageName(@RequestParam String stageName) { return service.getByStageName(stageName); }

    @GetMapping("/{id}")
    public ArtistResponse byId(@PathVariable Long id) { return service.getById(id); }
}
