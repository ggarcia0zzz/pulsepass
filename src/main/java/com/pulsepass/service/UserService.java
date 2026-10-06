package com.pulsepass.service;

import com.pulsepass.dto.CreateUserProfileDto;
import com.pulsepass.dto.RegisterUserDto;
import com.pulsepass.dto.UserDto;
import jakarta.validation.Valid;

import java.util.List;

public interface UserService {

    UserDto register(@Valid RegisterUserDto request);

    UserDto findById(Long id);

    UserDto findByEmail(String email);

    UserDto findByUsername(String username);

    List<UserDto> findAll();

    UserDto deactivate(Long id);

    UserDto createProfile(Long userId, @Valid CreateUserProfileDto request);
}
