package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    // "Hoy" congelado: 2026-09-28
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, userMapper, CLOCK);
    }

    private RegisterUserRequest request(LocalDate birthDate) {
        return new RegisterUserRequest(" andrea ", " Andrea@Email.com ", " Andrea ", "Lopez",
                "3000000000", "Santa Marta", birthDate);
    }

    private UserResponse response() {
        return new UserResponse(1L, "andrea", "andrea@email.com", true, "Andrea", "Lopez",
                "3000000000", "Santa Marta", LocalDate.of(2001, 3, 10));
    }

    @Test
    void register_validUser_savesActiveUserWithProfileAndNormalizedEmail() {
        // ARRANGE
        UserResponse expected = response();
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expected);

        // ACT
        UserResponse result = service.register(request(LocalDate.of(2001, 3, 10)));

        // ASSERT
        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("andrea");
        assertThat(saved.getEmail()).isEqualTo("andrea@email.com");   // BR-USER-002 (normalizado)
        assertThat(saved.isActive()).isTrue();                          // BR-USER-003
        assertThat(saved.getProfile()).isNotNull();                     // BR-USER-004
        assertThat(saved.getProfile().getFirstName()).isEqualTo("Andrea");
        assertThat(saved.getProfile().getBirthDate()).isEqualTo(LocalDate.of(2001, 3, 10));
        assertThat(saved.getProfile().getUser()).isSameAs(saved);
    }

    @Test
    void register_duplicatedUsername_throwsDuplicateAndDoesNotSave() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        // ACT + ASSERT (BR-USER-001)
        assertThatThrownBy(() -> service.register(request(LocalDate.of(2001, 3, 10))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("andrea");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicatedEmail_throwsDuplicateAndDoesNotSave() {
        // ARRANGE (el email llega con mayusculas y espacios, se busca normalizado)
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        // ACT + ASSERT (BR-USER-002)
        assertThatThrownBy(() -> service.register(request(LocalDate.of(2001, 3, 10))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("andrea@email.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_futureBirthDate_throwsBusinessRuleAndDoesNotSave() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        // ACT + ASSERT (BR-USER-005)
        assertThatThrownBy(() -> service.register(request(LocalDate.of(2026, 9, 29))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_birthDateToday_isAccepted() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(response());

        // ACT
        UserResponse result = service.register(request(LocalDate.of(2026, 9, 28)));

        // ASSERT
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findByEmail_existingUser_returnsResponse() {
        // ARRANGE
        User user = User.create("andrea", "andrea@email.com");
        UserResponse expected = response();
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        // ACT
        UserResponse result = service.findByEmail(" ANDREA@email.com ");

        // ASSERT
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findByEmail_unknownUser_throwsResourceNotFound() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(eq("nadie@email.com"))).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findByEmail("nadie@email.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByUsername_unknownUser_throwsResourceNotFound() {
        // ARRANGE
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> service.findByUsername("nadie"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByUsername_existingUser_returnsResponse() {
        // ARRANGE
        User user = User.create("andrea", "andrea@email.com");
        UserResponse expected = response();
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        // ACT
        UserResponse result = service.findByUsername("andrea");

        // ASSERT
        assertThat(result).isEqualTo(expected);
    }
}
