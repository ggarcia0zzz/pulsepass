package com.pulsepass.dto.response;

import java.util.List;

/** Información mínima del principal: sin correo, sin hash, sin secretos. */
public record AuthenticatedUserResponse(Long id, String username, List<String> roles) {
}
