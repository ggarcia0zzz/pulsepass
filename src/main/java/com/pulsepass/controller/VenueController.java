package com.pulsepass.controller;

import com.pulsepass.dto.VenueResponse;
import com.pulsepass.service.VenueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {
    private final VenueService service;

    public VenueController(VenueService service) { this.service = service; }

    @GetMapping("/active")
    public List<VenueResponse> active() { return service.listActive(); }

    @GetMapping("/{code}")
    public VenueResponse byCode(@PathVariable String code) { return service.getByCode(code); }
}
