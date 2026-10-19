package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Emite y valida JWT firmados (JWS) con HS256. Responsabilidad única: firma, issuer,
 * algoritmo, fechas y sujeto. NO consulta tickets ni ejecuta negocio.
 */
@Service
public class JwtService {

    private static final String EXPECTED_ALGORITHM = "HS256";
    private static final int MIN_SECRET_BYTES = 32;
    private static final Duration MAX_CLOCK_SKEW = Duration.ofSeconds(60);

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Clock clock;
    private final JwtParser parser;

    public JwtService(JwtProperties properties, Clock clock) {
        this.key = buildKey(properties.secretBase64());
        this.issuer = properties.issuer();
        this.accessTokenTtl = properties.accessTokenTtl();
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    /** Emite un JWT de acceso. El subject es el ID persistente e inmutable del usuario. */
    public String generateToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /**
     * Verifica criptográficamente la firma ANTES de confiar en los claims y devuelve el ID de usuario.
     *
     * @throws InvalidJwtException ante cualquier token inválido (nunca otra excepción)
     */
    public Long extractUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException("Empty token");
        }
        try {
            return parseAndValidate(token);
        } catch (InvalidJwtException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // ExpiredJwtException, MalformedJwtException, SignatureException, UnsupportedJwtException,
            // IncorrectClaimException, errores de decodificación, etc. Mensaje genérico: sin detalles internos.
            throw new InvalidJwtException("Invalid token", ex);
        }
    }

    private Long parseAndValidate(String token) {
        Jws<Claims> jws = parser.parseSignedClaims(token);

        // El parser acepta HS256/384/512 para una clave HMAC: se impone el algoritmo esperado.
        if (!EXPECTED_ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
            throw new InvalidJwtException("Unexpected signing algorithm");
        }

        Claims claims = jws.getPayload();
        if (isBlank(claims.getSubject()) || isBlank(claims.getId())
                || claims.getExpiration() == null || claims.getIssuedAt() == null) {
            throw new InvalidJwtException("Missing required claims");
        }
        if (claims.getIssuedAt().toInstant().isAfter(clock.instant().plus(MAX_CLOCK_SKEW))) {
            throw new InvalidJwtException("Token issued in the future");
        }
        return Long.valueOf(claims.getSubject()); // NumberFormatException -> InvalidJwtException (arriba)
    }

    private static SecretKey buildKey(String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "PULSEPASS_JWT_SECRET_BASE64 is required (Base64 of at least 32 random bytes)");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretBase64.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("The JWT secret must be valid Base64");
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("The JWT secret must be at least 32 bytes (256 bits)");
        }
        // Clave con nombre JCA exacto "HmacSHA256" (igual al algoritmo HS256 que se emite y se exige),
        // válida para cualquier longitud >= 32 bytes.
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
