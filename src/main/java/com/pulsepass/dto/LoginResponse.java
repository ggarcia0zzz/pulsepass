package com.pulsepass.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
