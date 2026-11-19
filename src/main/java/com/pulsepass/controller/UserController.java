package com.pulsepass.controller;

import com.pulsepass.dto.UserRegisterRequest;
import com.pulsepass.dto.UserResponse;
import com.pulsepass.security.AuthenticatedUser;
import com.pulsepass.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegisterRequest request) { return service.register(request); }

    @GetMapping("/by-email")
    public UserResponse byEmail(@RequestParam String email, @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.getByEmail(email, actor);
    }

    @GetMapping("/by-username")
    public UserResponse byUsername(@RequestParam String username, @AuthenticationPrincipal AuthenticatedUser actor) {
        return service.getByUsername(username, actor);
    }
}
