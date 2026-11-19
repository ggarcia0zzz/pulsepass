package com.pulsepass.security;

import com.pulsepass.config.JwtProperties;
import com.pulsepass.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final long ttlSeconds;

    public JwtService(JwtProperties props) {
        if (props.secretBase64() == null || props.secretBase64().isBlank()) {
            throw new IllegalStateException("Falta PULSEPASS_JWT_SECRET_BASE64");
        }
        byte[] bytes = Decoders.BASE64.decode(props.secretBase64());
        if (bytes.length < 32) throw new IllegalStateException("La clave JWT debe tener al menos 32 bytes");
        this.key = Keys.hmacShaKeyFor(bytes);
        this.issuer = props.issuer();
        this.ttlSeconds = props.expirationSeconds();
    }

    public long ttlSeconds() { return ttlSeconds; }

    /** Claims: sub (id del usuario), iss, iat, exp, jti. No lleva roles: se recargan de la BD en cada peticion. */
    public String generate(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .id(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
    }

    /** Valida firma, emisor y expiracion. Lanza JwtException/IllegalArgumentException si es invalido. */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer).build()
                .parseSignedClaims(token).getPayload();
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("Subject invalido");
        }
    }
}
