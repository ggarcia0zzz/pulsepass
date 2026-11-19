package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.dto.LoginRequest;
import com.pulsepass.dto.LoginResponse;
import com.pulsepass.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    /** Hash de relleno: se compara aunque el usuario no exista para no filtrar por tiempo de respuesta. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.dummyHash = encoder.encode("dummy-password-not-used");
    }

    /** Siempre el mismo 401 generico: usuario inexistente, password erroneo, sin password o cuenta inactiva. */
    public LoginResponse login(LoginRequest req) {
        String id = req.identifier().trim();
        User user = (id.contains("@") ? users.findByEmailIgnoreCase(id) : users.findByUsername(id)).orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyHash;
        boolean matches = encoder.matches(req.password(), hash);
        if (user == null || user.getPasswordHash() == null || !matches || !user.isActive()) {
            throw new BadCredentialsException("Credenciales invalidas");
        }
        return new LoginResponse(jwt.generate(user), "Bearer", jwt.ttlSeconds());
    }
}
