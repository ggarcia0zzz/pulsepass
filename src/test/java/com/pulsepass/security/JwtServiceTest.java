package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Clave y emisor exclusivos de la prueba (ficticios). Clave de 64 bytes para poder firmar también HS384/HS512. */
class JwtServiceTest {

    private static final byte[] KEY_BYTES = new byte[64];
    private static final byte[] OTHER_KEY_BYTES = new byte[64];

    static {
        for (int i = 0; i < 64; i++) {
            KEY_BYTES[i] = (byte) (i * 7 + 3);
            OTHER_KEY_BYTES[i] = (byte) (i * 11 + 5);
        }
    }

    private static final String SECRET = Encoders.BASE64.encode(KEY_BYTES);
    private static final String OTHER_SECRET = Encoders.BASE64.encode(OTHER_KEY_BYTES);
    private static final String ISSUER = "pulsepass-test";
    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static JwtService service(String secret, String issuer, Clock clock) {
        return new JwtService(new JwtProperties(issuer, secret, Duration.ofMinutes(15)), clock);
    }

    private final JwtService jwtService = service(SECRET, ISSUER, CLOCK);

    /** Misma clave que usa JwtService (HmacSHA256) para firmar/verificar HS256 en la prueba. */
    private static SecretKey key() {
        return new SecretKeySpec(KEY_BYTES, "HmacSHA256");
    }

    private static SecretKey key512() {
        return new SecretKeySpec(KEY_BYTES, "HmacSHA512");
    }

    // ---------- emisión ----------

    @Test
    @DisplayName("Emite un JWT con sub, iss, iat, exp (15 min) y jti, y firma HS256 válida")
    void generateToken_containsMinimumClaims() {
        String token = jwtService.generateToken(42L);

        var jws = Jwts.parser().verifyWith(key()).clock(() -> Date.from(NOW)).build().parseSignedClaims(token);
        Claims claims = jws.getPayload();

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plusSeconds(900));
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.get("password")).isNull();
        assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("Dos tokens del mismo usuario tienen jti distinto")
    void generateToken_jtiIsRandom() {
        String first = jwtService.generateToken(1L);
        String second = jwtService.generateToken(1L);

        assertThat(first).isNotEqualTo(second);
    }

    // ---------- validación ----------

    @Test
    void extractUserId_validToken_returnsSubject() {
        assertThat(jwtService.extractUserId(jwtService.generateToken(42L))).isEqualTo(42L);
    }

    @Test
    @DisplayName("Token expirado -> InvalidJwtException")
    void extractUserId_expiredToken_isRejected() {
        String token = jwtService.generateToken(42L);
        JwtService sixteenMinutesLater = service(SECRET, ISSUER, Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

        assertThatThrownBy(() -> sixteenMinutesLater.extractUserId(token)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Token manipulado -> InvalidJwtException")
    void extractUserId_tamperedToken_isRejected() {
        String token = jwtService.generateToken(42L);
        int index = token.length() - 10;
        char replacement = token.charAt(index) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, index) + replacement + token.substring(index + 1);

        assertThatThrownBy(() -> jwtService.extractUserId(tampered)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Payload alterado (otro sub) con la firma original -> InvalidJwtException")
    void extractUserId_swappedPayload_isRejected() {
        String[] original = jwtService.generateToken(42L).split("\\.");
        String[] other = jwtService.generateToken(1L).split("\\.");
        String forged = original[0] + "." + other[1] + "." + original[2];

        assertThatThrownBy(() -> jwtService.extractUserId(forged)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Emisor distinto -> InvalidJwtException")
    void extractUserId_wrongIssuer_isRejected() {
        String token = service(SECRET, "otro-emisor", CLOCK).generateToken(42L);

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Firmado con otra clave -> InvalidJwtException")
    void extractUserId_wrongKey_isRejected() {
        String token = service(OTHER_SECRET, ISSUER, CLOCK).generateToken(42L);

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("alg=none / token sin firmar -> InvalidJwtException")
    void extractUserId_unsignedToken_isRejected() {
        String unsigned = Jwts.builder()
                .subject("42").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(unsigned)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Algoritmo inesperado (HS512 con la clave correcta) -> InvalidJwtException")
    void extractUserId_unexpectedAlgorithm_isRejected() {
        String hs512 = Jwts.builder()
                .subject("42").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .signWith(key512(), Jwts.SIG.HS512)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(hs512)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void extractUserId_missingSubject_isRejected() {
        String token = Jwts.builder()
                .issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .signWith(key(), Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void extractUserId_nonNumericSubject_isRejected() {
        String token = Jwts.builder()
                .subject("no-es-un-id").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .signWith(key(), Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void extractUserId_missingExpiration_isRejected() {
        String token = Jwts.builder()
                .subject("42").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(NOW))
                .signWith(key(), Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    void extractUserId_missingJti_isRejected() {
        String token = Jwts.builder()
                .subject("42").issuer(ISSUER)
                .issuedAt(Date.from(NOW)).expiration(Date.from(NOW.plusSeconds(900)))
                .signWith(key(), Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Token emitido en el futuro (más allá de la tolerancia) -> InvalidJwtException")
    void extractUserId_issuedInTheFuture_isRejected() {
        String token = Jwts.builder()
                .subject("42").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(NOW.plus(Duration.ofHours(1)))).expiration(Date.from(NOW.plus(Duration.ofHours(2))))
                .signWith(key(), Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(InvalidJwtException.class);
    }

    @Test
    @DisplayName("Entradas vacías o basura nunca lanzan otra excepción que InvalidJwtException")
    void extractUserId_garbage_isRejectedWithoutOtherExceptions() {
        for (String garbage : new String[]{null, "", "   ", "abc", "a.b.c", "....", "%%%.%%%.%%%", "Bearer x"}) {
            assertThatThrownBy(() -> jwtService.extractUserId(garbage))
                    .as("token=%s", garbage)
                    .isInstanceOf(InvalidJwtException.class);
        }
    }

    // ---------- configuración de la clave ----------

    @Test
    @DisplayName("Clave ausente, no Base64 o menor de 32 bytes -> la aplicación no arranca")
    void constructor_invalidSecret_failsFast() {
        String weak = Encoders.BASE64.encode(new byte[16]);

        assertThatThrownBy(() -> service(null, ISSUER, CLOCK)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("   ", ISSUER, CLOCK)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service("esto no es base64 !!!", ISSUER, CLOCK)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service(weak, ISSUER, CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes")
                .hasMessageNotContaining(weak);
    }
}
