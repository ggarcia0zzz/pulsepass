package com.pulsepass.service;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.UserRegisterRequest;
import com.pulsepass.dto.UserResponse;
import com.pulsepass.exception.ConflictException;
import com.pulsepass.exception.ForbiddenException;
import com.pulsepass.exception.NotFoundException;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.AuthenticatedUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    /** Registro publico: el rol siempre es USER. */
    public UserResponse register(UserRegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        String username = req.username().trim();
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("El correo ya esta registrado");
        }
        if (users.findByUsername(username).isPresent()) {
            throw new ConflictException("El nombre de usuario ya esta en uso");
        }
        User user = User.register(username, email, encoder.encode(req.password()));
        user.assignProfile(UserProfile.create(req.firstName().trim(), req.lastName().trim(),
                blankToNull(req.phone()), blankToNull(req.city()), req.birthDate(), user));
        return UserResponse.from(users.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthenticatedUser actor) {
        return users.findById(actor.id()).map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    /** Propietario o ADMIN. Un usuario ajeno recibe 403 exista o no el recurso (no se filtra existencia). */
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email, AuthenticatedUser actor) {
        if (!actor.owns(email) && !actor.isAdmin()) throw new ForbiddenException("Acceso denegado");
        return users.findByEmailIgnoreCase(email.trim()).map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public UserResponse getByUsername(String username, AuthenticatedUser actor) {
        if (!actor.username().equals(username) && !actor.isAdmin()) throw new ForbiddenException("Acceso denegado");
        return users.findByUsername(username).map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
