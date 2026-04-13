package com.dashboard.oauth.service;

import com.dashboard.oauth.config.RsaKeyPair;
import com.dashboard.oauth.environment.JWTProperties;
import com.dashboard.oauth.environment.OidcProperties;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.repository.IServerKeyRepository;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JWT Service")
class JwtServiceTest {

    private JwtService jwtService;
    private static final String TEST_SECRET = "dGVzdHNlY3JldGtleXRoYXRpc2xvbmdlbm91Z2hmb3JobWFjc2hhMjU2YWxnb3JpdGht";
    private static final Long TEST_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() throws Exception {
        JWTProperties jwtProperties = new JWTProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpiration(TEST_EXPIRATION);
        OidcProperties oidcProperties = new OidcProperties();
        oidcProperties.setIssuer("http://localhost:8081");
        IServerKeyRepository repo = mock(IServerKeyRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        jwtService = new JwtService(jwtProperties, oidcProperties, new RsaKeyPair(repo));
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void generateToken_shouldCreateValidToken() {
        UserInfo userInfo = createTestUserInfo();

        String token = jwtService.generateToken(userInfo, null, null);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract username from JWT token")
    void extractUsername_shouldReturnCorrectEmail() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo, null, null);

        String extractedUsername = jwtService.extractUsername(token);

        assertEquals("test@example.com", extractedUsername);
    }

    @Test
    @DisplayName("Should extract claims from JWT token")
    void extractClaim_shouldReturnUserId() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo, null, null);

        String userId = jwtService.extractClaim(token, Claims::getSubject);

        assertNotNull(userId);
    }

    @Test
    @DisplayName("Should extract claims from JWT token")
    void extractClaim_shouldReturnGrants() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo, null, null);

        @SuppressWarnings("unchecked")
        List<String> grants = jwtService.extractClaim(token, claims -> (List<String>) claims.get("grants"));

        assertNotNull(grants);
        assertTrue(grants.contains("READ"));
        assertTrue(grants.contains("WRITE"));
    }

    @Test
    @DisplayName("Should extract claims from JWT token")
    void extractClaim_shouldReturnExpiration() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo, null, null);

        java.util.Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertNotNull(expiration);
        assertTrue(expiration.after(new java.util.Date()));
    }

    @Test
    @DisplayName("Should validate JWT token")
    void validateToken_shouldReturnTrueForValidToken() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo, null, null);
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        Boolean isValid = jwtService.validateToken(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should validate JWT token")
    void validateToken_shouldReturnFalseForWrongUsername() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo, null, null);
        UserDetails userDetails = User.builder()
                .username("wrong@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        Boolean isValid = jwtService.validateToken(token, userDetails);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should validate JWT token")
    void validateToken_shouldReturnFalseForExpiredToken() throws Exception {
        // Create a new JwtService with a very short expiration
        JWTProperties shortExpirationProps = new JWTProperties();
        shortExpirationProps.setSecret(TEST_SECRET);
        shortExpirationProps.setExpiration(1L); // 1ms
        OidcProperties oidcProperties = new OidcProperties();
        oidcProperties.setIssuer("http://localhost:8081");
        IServerKeyRepository repo = mock(IServerKeyRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        JwtService shortExpirationJwtService = new JwtService(shortExpirationProps, oidcProperties, new RsaKeyPair(repo));

        UserInfo userInfo = createTestUserInfo();
        String token = shortExpirationJwtService.generateToken(userInfo, null, null);

        // Wait for token to expire
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        // Should throw ExpiredJwtException when trying to validate
        assertThrows(ExpiredJwtException.class, () -> shortExpirationJwtService.validateToken(token, userDetails));
    }

    @Test
    @DisplayName("Should handle empty grants")
    void generateToken_shouldHandleEmptyGrants() {
        UserInfo userInfo = new UserInfo();
        userInfo.set_id(new ObjectId());
        userInfo.setEmail("test@example.com");

        Role role = new Role();
        role.set_id(new ObjectId());
        role.setName("ROLE_USER");
        role.setGrants(new ArrayList<>());

        userInfo.setRole(List.of(role));

        String token = jwtService.generateToken(userInfo, null, null);

        assertNotNull(token);
        @SuppressWarnings("unchecked")
        List<String> grants = jwtService.extractClaim(token, claims -> (List<String>) claims.get("grants"));
        assertTrue(grants.isEmpty());
    }

    @Test
    @DisplayName("Should deduplicate grants")
    void generateToken_shouldDeduplicateGrants() {
        UserInfo userInfo = new UserInfo();
        userInfo.set_id(new ObjectId());
        userInfo.setEmail("test@example.com");

        Grant readGrant = new Grant();
        readGrant.set_id(new ObjectId());
        readGrant.setName("READ");

        Role role1 = new Role();
        role1.set_id(new ObjectId());
        role1.setName("ROLE_USER");
        role1.setGrants(List.of(readGrant));

        Role role2 = new Role();
        role2.set_id(new ObjectId());
        role2.setName("ROLE_ADMIN");
        role2.setGrants(List.of(readGrant)); // Same grant

        userInfo.setRole(List.of(role1, role2));

        String token = jwtService.generateToken(userInfo, null, null);

        @SuppressWarnings("unchecked")
        List<String> grants = jwtService.extractClaim(token, claims -> (List<String>) claims.get("grants"));

        // Should only contain one READ grant (deduplicated)
        assertEquals(1, grants.stream().filter(g -> g.equals("READ")).count());
    }

    private UserInfo createTestUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.set_id(new ObjectId());
        userInfo.setEmail("test@example.com");

        Grant readGrant = new Grant();
        readGrant.set_id(new ObjectId());
        readGrant.setName("READ");

        Grant writeGrant = new Grant();
        writeGrant.set_id(new ObjectId());
        writeGrant.setName("WRITE");

        Role role = new Role();
        role.set_id(new ObjectId());
        role.setName("ROLE_USER");
        role.setGrants(List.of(readGrant, writeGrant));

        userInfo.setRole(List.of(role));

        return userInfo;
    }
}
