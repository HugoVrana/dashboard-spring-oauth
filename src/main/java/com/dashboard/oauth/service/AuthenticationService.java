package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.datatransferobjects.AuthResponse;
import com.dashboard.oauth.datatransferobjects.LoginRequest;
import com.dashboard.oauth.datatransferobjects.RegisterRequest;
import com.dashboard.oauth.datatransferobjects.UserInfo;
import com.dashboard.oauth.model.entities.RefreshToken;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
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

    public UserInfo register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationServiceException("The user with email : " + request.getEmail() + " already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        user = userRepository.save(user);

        UserInfo info = new UserInfo();
        info.setId(user.get_id());
        info.setEmail(user.getEmail());
        info.setRole(user.getRoles());

        return info;
    }

    public AuthResponse login(LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        }
        catch (Exception e) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = optionalUser.get();

        UserInfo info = new UserInfo();
        info.setId(user.get_id());
        info.setEmail(user.getEmail());
        info.setRole(user.getRoles());

        String accessToken = jwtService.generateToken(info);
        String refreshToken = createRefreshToken(user);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtExpiration);
        response.setUser(info);

        return response;
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

        UserInfo info = new UserInfo();
        info.setId(user.get_id());
        info.setEmail(user.getEmail());
        info.setRole(user.getRoles());
        String accessToken = jwtService.generateToken(info);


        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenStr);
        response.setExpiresIn(jwtExpiration);
        response.setUser(info);

        return response;
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
