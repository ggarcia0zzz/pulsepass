package com.pulsepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(
        String secretBase64,
        @DefaultValue("pulsepass-api") String issuer,
        @DefaultValue("900") long expirationSeconds) {
}
