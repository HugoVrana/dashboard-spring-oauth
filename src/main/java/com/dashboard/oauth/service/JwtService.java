package com.dashboard.oauth.service;

import com.dashboard.oauth.config.RsaKeyPair;
import com.dashboard.oauth.environment.JWTProperties;
import com.dashboard.oauth.environment.OidcProperties;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.service.interfaces.IJwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@Scope("singleton")
@RequiredArgsConstructor
public class JwtService implements IJwtService {

    private final JWTProperties jwtProperties;
    private final OidcProperties oidcProperties;
    private final RsaKeyPair rsaKeyPair;

    @Override
    public String generateToken(UserInfo userDetails, List<Grant> allowedGrants, String clientId) {
        Set<String> allowedGrantNames = allowedGrants == null ? null : allowedGrants.stream()
                .map(Grant::getName)
                .collect(java.util.stream.Collectors.toSet());

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", userDetails.getEmail());
        if (clientId != null) {
            claims.put("client_id", clientId);
        }
        claims.put("grants", userDetails.getRole().stream()
                .flatMap(role -> role.getGrants().stream())
                .map(Grant::getName)
                .filter(name -> allowedGrantNames == null || allowedGrantNames.contains(name))
                .distinct()
                .toList());

        Instant now = Instant.now();
        Instant expiryInstant = now.plusMillis(jwtProperties.getExpiration());

        return Jwts.builder()
                .header().add("kid", rsaKeyPair.getKid()).and()
                .claims(claims)
                .issuer(oidcProperties.getIssuer())
                .subject(userDetails.getId().toHexString())
                .audience().add(oidcProperties.getIssuer()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryInstant))
                .signWith(rsaKeyPair.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    @Override
    public String generateIdToken(UserInfo userInfo, String clientId, String nonce) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(jwtProperties.getExpiration());

        var builder = Jwts.builder()
                .header().add("kid", rsaKeyPair.getKid()).and()
                .issuer(oidcProperties.getIssuer())
                .subject(userInfo.getId().toHexString())
                .claim("aud", clientId)
                .claim("email", userInfo.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp));

        if (nonce != null && !nonce.isBlank()) {
            builder = builder.claim("nonce", nonce);
        }

        return builder.signWith(rsaKeyPair.getPrivateKey(), Jwts.SIG.RS256).compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(rsaKeyPair.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    @Override
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean usernameMatch = username.equals(userDetails.getUsername());
        boolean isTokenExpired = extractClaim(token, Claims::getExpiration).before(new Date());
        return (usernameMatch && !isTokenExpired);
    }
}
