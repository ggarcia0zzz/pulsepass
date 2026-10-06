package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.UserService;
import com.pulsepass.shared.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;

@Service
@Validated
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Clock clock;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, Clock clock) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {

        String username = TextNormalizer.text(request.username());
        // El email se guarda en minusculas: el UNIQUE de Postgres distingue mayusculas
        // y asi "Ana@mail.com" y "ana@mail.com" no pueden convivir (BR-USER-002).
        String email = TextNormalizer.email(request.email());

        // BR-USER-001: username unico
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User", "username", username);
        }

        // BR-USER-002: email unico ignorando mayusculas/minusculas
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        // BR-USER-005: birthDate no puede ser futura
        if (request.birthDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("Birth date cannot be in the future: " + request.birthDate());
        }

        // BR-USER-003: User.create() inicia con active = true
        User user = User.create(username, email);

        // BR-USER-004: User y UserProfile se crean en la misma transaccion
        // (UserProfile se persiste por cascade = ALL desde User)
        UserProfile profile = UserProfile.create(
                TextNormalizer.text(request.firstName()),
                TextNormalizer.text(request.lastName()),
                TextNormalizer.text(request.phone()),
                TextNormalizer.optionalText(request.city()),
                request.birthDate(),
                user
        );
        user.assignProfile(profile);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse findByEmail(String email) {
        String normalized = TextNormalizer.email(email);

        return userRepository
                .findByEmailIgnoreCase(normalized)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", normalized));
    }

    @Override
    public UserResponse findByUsername(String username) {
        String normalized = TextNormalizer.text(username);

        return userRepository
                .findByUsername(normalized)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", normalized));
    }
}
