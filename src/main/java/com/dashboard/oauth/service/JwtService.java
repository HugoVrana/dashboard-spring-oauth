package com.dashboard.oauth.service;

import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.service.interfaces.IJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService implements IJwtService {

    @Value("${JWT.SECRET}")
    private String secret;

    @Value("${JWT.EXPIRATION}")
    private Long expiration;

    public String generateToken(UserInfo userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getId());
        claims.put("grants", userDetails.getRole().stream()
                .flatMap(role -> role.getGrants().stream())
                .map(Grant::getName)
                .distinct()
                .toList()
        );

        Instant now = Instant.now();
        Instant expiryInstant = now.plusMillis(expiration);
        SecretKey s = getSignKey();

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryInstant))
                .signWith(s)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean usernameMatch = username.equals(userDetails.getUsername());
        boolean isTokenExpired = extractClaim(token, Claims::getExpiration).before(new Date());
        return (usernameMatch && !isTokenExpired);
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
