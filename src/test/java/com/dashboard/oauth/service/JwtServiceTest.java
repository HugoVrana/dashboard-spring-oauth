package com.dashboard.oauth.service;

import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String TEST_SECRET = "dGVzdHNlY3JldGtleXRoYXRpc2xvbmdlbm91Z2hmb3JobWFjc2hhMjU2YWxnb3JpdGht";
    private static final Long TEST_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", TEST_EXPIRATION);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        UserInfo userInfo = createTestUserInfo();

        String token = jwtService.generateToken(userInfo);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length); // JWT has 3 parts
    }

    @Test
    void extractUsername_shouldReturnCorrectEmail() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo);

        String extractedUsername = jwtService.extractUsername(token);

        assertEquals("test@example.com", extractedUsername);
    }

    @Test
    void extractClaim_shouldReturnUserId() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo);

        // The userId is stored as ObjectId which serializes to a map with $oid
        Object userId = jwtService.extractClaim(token, claims -> claims.get("userId"));

        assertNotNull(userId);
    }

    @Test
    void extractClaim_shouldReturnGrants() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo);

        @SuppressWarnings("unchecked")
        List<String> grants = jwtService.extractClaim(token, claims -> (List<String>) claims.get("grants"));

        assertNotNull(grants);
        assertTrue(grants.contains("READ"));
        assertTrue(grants.contains("WRITE"));
    }

    @Test
    void extractClaim_shouldReturnExpiration() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo);

        java.util.Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertNotNull(expiration);
        assertTrue(expiration.after(new java.util.Date()));
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo);
        UserDetails userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        Boolean isValid = jwtService.validateToken(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForWrongUsername() {
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo);
        UserDetails userDetails = User.builder()
                .username("wrong@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        Boolean isValid = jwtService.validateToken(token, userDetails);

        assertFalse(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        // Set a very short expiration
        ReflectionTestUtils.setField(jwtService, "expiration", 1L); // 1ms
        UserInfo userInfo = createTestUserInfo();
        String token = jwtService.generateToken(userInfo);

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
        assertThrows(ExpiredJwtException.class, () -> jwtService.validateToken(token, userDetails));
    }

    @Test
    void generateToken_shouldHandleEmptyGrants() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(new ObjectId());
        userInfo.setEmail("test@example.com");

        Role role = new Role();
        role.set_id(new ObjectId());
        role.setName("ROLE_USER");
        role.setGrants(new ArrayList<>());

        userInfo.setRole(List.of(role));

        String token = jwtService.generateToken(userInfo);

        assertNotNull(token);
        @SuppressWarnings("unchecked")
        List<String> grants = jwtService.extractClaim(token, claims -> (List<String>) claims.get("grants"));
        assertTrue(grants.isEmpty());
    }

    @Test
    void generateToken_shouldDeduplicateGrants() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(new ObjectId());
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

        String token = jwtService.generateToken(userInfo);

        @SuppressWarnings("unchecked")
        List<String> grants = jwtService.extractClaim(token, claims -> (List<String>) claims.get("grants"));

        // Should only contain one READ grant (deduplicated)
        assertEquals(1, grants.stream().filter(g -> g.equals("READ")).count());
    }

    private UserInfo createTestUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(new ObjectId());
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
