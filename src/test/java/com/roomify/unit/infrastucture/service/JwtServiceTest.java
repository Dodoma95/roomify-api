package com.roomify.unit.infrastucture.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.roomify.infrastucture.service.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private static final String SECRET = "my-super-secret-key-that-is-at-least-32-characters-long";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET", SECRET);
    }

    // --- generateAccessToken ---

    @Test
    void generateAccessToken_returnsTokenWithCorrectSubject() {
        UserDetails user = mockUser("alice@test.com", List.of("ROLE_USER"));

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@test.com");
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateAccessToken_includesRolesClaim() {
        UserDetails user = mockUser("alice@test.com", List.of("ROLE_USER", "ROLE_ADMIN"));

        String token = jwtService.generateAccessToken(user);

        List<String> roles = (List<String>) parseClaims(token).get("roles");
        assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void generateAccessToken_expiresInApproximately15Minutes() {
        UserDetails user = mockUser("alice@test.com", List.of());

        long before = System.currentTimeMillis();
        String token = jwtService.generateAccessToken(user);
        long after = System.currentTimeMillis();

        Date expiration = parseClaims(token).getExpiration();
        long durationMs = 1000L * 60 * 15;
        // JWT stores dates as epoch seconds — actual value may be up to 999ms below the computed expiry
        assertThat(expiration.getTime())
                .isBetween(before + durationMs - 1000, after + durationMs);
    }

    // --- generateRefreshToken ---

    @Test
    void generateRefreshToken_returnsTokenWithCorrectSubject() {
        UserDetails user = mockUser("bob@test.com", List.of());

        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("bob@test.com");
    }

    @Test
    void generateRefreshToken_doesNotIncludeRolesClaim() {
        UserDetails user = mockUser("bob@test.com", List.of("ROLE_USER"));

        String token = jwtService.generateRefreshToken(user);

        assertThat(parseClaims(token).get("roles")).isNull();
    }

    @Test
    void generateRefreshToken_expiresInApproximately7Days() {
        UserDetails user = mockUser("bob@test.com", List.of());

        long before = System.currentTimeMillis();
        String token = jwtService.generateRefreshToken(user);
        long after = System.currentTimeMillis();

        Date expiration = parseClaims(token).getExpiration();
        long durationMs = 1000L * 60 * 60 * 24 * 7;
        assertThat(expiration.getTime())
                .isBetween(before + durationMs - 1000, after + durationMs);
    }

    // --- isTokenValid ---

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        UserDetails user = mockUser("alice@test.com", List.of("ROLE_USER"));
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseWhenUsernameDoesNotMatch() {
        UserDetails user = mockUser("alice@test.com", List.of());
        UserDetails otherUser = mockUser("bob@test.com", List.of());
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForExpiredToken() {
        String expiredToken = Jwts.builder()
                .subject("alice@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 5_000))
                .signWith(buildSigningKey())
                .compact();
        UserDetails user = mockUser("alice@test.com", List.of());

        assertThat(jwtService.isTokenValid(expiredToken, user)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForMalformedToken() {
        UserDetails user = mockUser("alice@test.com", List.of());

        assertThat(jwtService.isTokenValid("not.a.jwt.token", user)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalseForTokenSignedWithWrongKey() {
        String otherSecret = "another-totally-different-secret-key-32chars";
        String foreignToken = Jwts.builder()
                .subject("alice@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(Encoders.BASE64.encode(otherSecret.getBytes()))))
                .compact();
        UserDetails user = mockUser("alice@test.com", List.of());

        assertThat(jwtService.isTokenValid(foreignToken, user)).isFalse();
    }

    // --- extractUsername ---

    @Test
    void extractUsername_returnsCorrectUsername() {
        UserDetails user = mockUser("carol@test.com", List.of());
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("carol@test.com");
    }

    @Test
    void extractUsername_throwsForInvalidToken() {
        assertThatThrownBy(() -> jwtService.extractUsername("invalid-token"))
                .isInstanceOf(Exception.class);
    }

    // --- helpers ---

    private UserDetails mockUser(String username, List<String> roles) {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn(username);
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) () -> role)
                .toList();
        //noinspection unchecked
        when(user.getAuthorities()).thenReturn((Collection) authorities);
        return user;
    }

    private io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(buildSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey buildSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(Encoders.BASE64.encode(SECRET.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
