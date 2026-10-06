package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import jakarta.validation.Valid;

public interface UserService {

    UserResponse register(@Valid RegisterUserRequest request);

    UserResponse findByEmail(String email);

    UserResponse findByUsername(String username);
}
