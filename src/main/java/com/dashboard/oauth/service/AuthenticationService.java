package com.dashboard.oauth.service;

import com.dashboard.oauth.datatransferobjects.AuthResponse;
import com.dashboard.oauth.datatransferobjects.LoginRequest;
import com.dashboard.oauth.datatransferobjects.RegisterRequest;
import com.dashboard.oauth.datatransferobjects.UserInfo;
import com.dashboard.oauth.model.entities.Audit;
import com.dashboard.oauth.model.entities.RefreshToken;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {
    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        Audit a = new Audit();
        a.setCreatedAt(Instant.now());
        user.setAudit(a);
        user = userRepository.save(user);

        String accessToken = jwtService.generateToken(new UserInfo(user.get_id(), user.getEmail(), user.getRoles()));
        String refreshToken = createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtExpiration,
                UserInfo.fromUser(user)
        );
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(UserInfo.fromUser(user));
        String refreshToken = createRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtExpiration,
                UserInfo.fromUser(user)
        );
    }

    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().toInstant().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        User user = userRepository.findById(new ObjectId(refreshToken.getUserId()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateToken(UserInfo.fromUser(user));

        return new AuthResponse(
                accessToken,
                refreshTokenStr,
                jwtExpiration,
                UserInfo.fromUser(user)
        );
    }

    private String createRefreshToken(User user) {
        // Delete existing refresh tokens for this user
        refreshTokenRepository.deleteByUserId(user.get_id().toHexString());

        String tokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken(
                tokenValue,
                user.get_id().toHexString(),
                Instant.now().plusMillis(refreshExpiration)
        );

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
