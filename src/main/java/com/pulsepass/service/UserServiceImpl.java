package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.CreateUserProfileDto;
import com.pulsepass.dto.RegisterUserDto;
import com.pulsepass.dto.UserDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ProfileAlreadyExistsException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.shared.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto register(RegisterUserDto request) {

        String username = TextNormalizer.text(request.username());
        String email = TextNormalizer.email(request.email());

        // FR-USR-002: username y email unicos.
        // El email se normaliza a minusculas porque el UNIQUE de Postgres es sensible a
        // mayusculas y findByEmailIgnoreCase se usa para buscar; normalizar al guardar evita
        // que "Ana@mail.com" y "ana@mail.com" convivan como registros distintos.
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User", "username", username);
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        User user = User.create(username, email);

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserDto findById(Long id) {
        return userRepository
                .findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    public UserDto findByEmail(String email) {
        String normalized = TextNormalizer.email(email);

        return userRepository
                .findByEmailIgnoreCase(normalized)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", normalized));
    }

    @Override
    public UserDto findByUsername(String username) {
        String normalized = TextNormalizer.text(username);

        return userRepository
                .findByUsername(normalized)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", normalized));
    }

    @Override
    public List<UserDto> findAll() {
        return userRepository
                .findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UserDto deactivate(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.deactivate();

        return userMapper.toDto(user);
    }

    @Override
    @Transactional
    public UserDto createProfile(Long userId, CreateUserProfileDto request) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // BR-004: un usuario puede tener como maximo un UserProfile
        if (user.getProfile() != null) {
            throw new ProfileAlreadyExistsException(userId);
        }

        UserProfile profile = UserProfile.create(
                TextNormalizer.text(request.firstName()),
                TextNormalizer.text(request.lastName()),
                TextNormalizer.text(request.phone()),
                TextNormalizer.optionalText(request.city()),
                request.birthDate(),
                user
        );

        user.assignProfile(profile);

        // No hace falta repository.save(): user ya esta gestionado por el
        // Persistence Context y profile tiene cascade = ALL desde User.
        return userMapper.toDto(user);
    }
}
