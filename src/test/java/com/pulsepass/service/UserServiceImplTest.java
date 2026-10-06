package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.dto.CreateUserProfileDto;
import com.pulsepass.dto.RegisterUserDto;
import com.pulsepass.dto.UserDto;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ProfileAlreadyExistsException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void shouldRegisterUserNormalizingEmail() {
        var request = new RegisterUserDto(" andrea ", " Andrea@Example.com ");
        var expected = new UserDto(1L, "andrea", "andrea@example.com", true, null);

        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenReturn(expected);

        UserDto result = service.register(request);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("andrea@example.com");
    }

    @Test
    void shouldRejectDuplicatedUsername() {
        var request = new RegisterUserDto("andrea", "andrea@example.com");

        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicatedEmail() {
        var request = new RegisterUserDto("andrea", "andrea@example.com");

        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldCreateProfileForUserWithoutOne() {
        User user = User.create("andrea", "andrea@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(null);

        service.createProfile(1L, new CreateUserProfileDto(
                "Andrea", "Lopez", "3000000000", "Santa Marta", LocalDate.of(1998, 5, 10)));

        assertThat(user.getProfile()).isNotNull();
        assertThat(user.getProfile().getFirstName()).isEqualTo("Andrea");
    }

    @Test
    void shouldRejectSecondProfileForSameUser() {
        User user = User.create("andrea", "andrea@example.com");
        user.assignProfile(com.pulsepass.domain.UserProfile.create(
                "Andrea", "Lopez", "3000000000", "Santa Marta", LocalDate.of(1998, 5, 10), user));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var secondRequest = new CreateUserProfileDto(
                "Andrea", "Lopez", "3000000000", "Santa Marta", LocalDate.of(1998, 5, 10));

        assertThatThrownBy(() -> service.createProfile(1L, secondRequest))
                .isInstanceOf(ProfileAlreadyExistsException.class);
    }

    @Test
    void shouldThrowWhenUserDoesNotExistForProfile() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new CreateUserProfileDto(
                "Andrea", "Lopez", "3000000000", "Santa Marta", LocalDate.of(1998, 5, 10));

        assertThatThrownBy(() -> service.createProfile(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
