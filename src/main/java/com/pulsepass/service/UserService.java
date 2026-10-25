package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UserService {

    UserResponse register(@Valid RegisterUserRequest request);

    @PreAuthorize("@authorizationService.canViewUserByEmail(#email)")
    UserResponse findByEmail(String email);

    @PreAuthorize("@authorizationService.canViewUserByUsername(#username)")
    UserResponse findByUsername(String username);
}
