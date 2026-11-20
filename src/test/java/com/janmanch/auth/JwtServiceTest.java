package com.janmanch.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    @Test
    void tokenRoundTripReturnsTheSameUserId() {
        JwtService service = new JwtService(SECRET, 60);
        assertEquals(42L, service.parseUserId(service.createToken(42L)));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService issuer = new JwtService("another-secret-that-is-also-32-characters-long", 60);
        JwtService verifier = new JwtService(SECRET, 60);
        assertThrows(JwtException.class, () -> verifier.parseUserId(issuer.createToken(42L)));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, -1);
        assertThrows(JwtException.class, () -> service.parseUserId(service.createToken(42L)));
    }
}
