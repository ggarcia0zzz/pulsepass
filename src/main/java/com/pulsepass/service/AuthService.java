package com.pulsepass.service;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.security.SecurityPrincipal;

/** Autenticación y alta de cuentas con credenciales. No reemplaza a UserService. */
public interface AuthService {

    /** Crea User + UserProfile + credencial BCrypt + ROLE_USER en una única transacción. */
    UserResponse register(RegisterUserRequest request);

    /** @throws org.springframework.security.authentication.BadCredentialsException ante cualquier fallo (401 genérico) */
    AuthResponse login(LoginRequest request);

    AuthenticatedUserResponse me(SecurityPrincipal principal);
}
