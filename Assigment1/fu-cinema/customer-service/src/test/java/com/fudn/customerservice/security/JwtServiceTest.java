package com.fudn.customerservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private static final String SECRET = "fu-cinema-booking-system-secret-key-2026-mss301";

    private NimbusJwtDecoder decoder(String secret) {
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void tokenHasVerifiedIdentityRoleAndLifetime() {
        JwtService service = new JwtService(SECRET, 60);
        Instant before = Instant.now().minusSeconds(1);
        Jwt jwt = decoder(SECRET).decode(service.generateToken(7L, "customer@example.com", "CUSTOMER"));
        assertEquals("customer@example.com", jwt.getSubject());
        assertEquals(7L, ((Number) jwt.getClaims().get("uid")).longValue());
        assertEquals("CUSTOMER", jwt.getClaimAsString("role"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertTrue(jwt.getIssuedAt().isAfter(before));
        assertEquals(3600, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
        assertEquals(3600, service.getExpirationSeconds());
    }

    @Test
    void wrongSecretIsRejected() {
        String token = new JwtService(SECRET, 60).generateToken(1L, "an@gmail.com", "CUSTOMER");
        assertThrows(JwtException.class, () -> decoder(SECRET + "wrong").decode(token));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant now = Instant.now();
        SignedJWT expired = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder().subject("an@gmail.com")
                        .issueTime(Date.from(now.minusSeconds(180)))
                        .expirationTime(Date.from(now.minusSeconds(120))).build());
        expired.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        String token = expired.serialize();
        assertThrows(JwtException.class, () -> decoder(SECRET).decode(token));
    }

    @Test
    void seededPasswordsMatchDocumentedTestAccount() {
        String seedHash = "$2a$10$dmoDdVpWYdqLarqBfkYQteoq1YORLC5LLMd55bpomZ3EarS/vtjtW";
        assertTrue(new BCryptPasswordEncoder().matches("123456", seedHash));
        assertFalse(new BCryptPasswordEncoder().matches("wrong-password", seedHash));
    }
}
