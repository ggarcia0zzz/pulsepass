package com.pulsepass.controller;

import com.pulsepass.dto.LoginRequest;
import com.pulsepass.dto.LoginResponse;
import com.pulsepass.dto.UserResponse;
import com.pulsepass.security.AuthService;
import com.pulsepass.security.AuthenticatedUser;
import com.pulsepass.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final UserService users;

    public AuthController(AuthService auth, UserService users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser actor) { return users.me(actor); }
}
