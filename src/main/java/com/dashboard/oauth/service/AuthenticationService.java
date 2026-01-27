package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.RefreshToken;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.entities.VerificationToken;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IJwtService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {
    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final IUserInfoMapper userInfoMapper;
    private final IRoleMapper roleMapper;
    private final IGrantMapper grantMapper;
    private final EmailProperties emailProperties;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    public User register(@NotNull RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationServiceException("The user with email : " + request.getEmail() + " already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new ArrayList<>());
        user.setEmailVerified(false);

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setCreatedAt(Instant.now());
        verificationToken.setExpiryDate(Instant.now().plusMillis(emailProperties.getVerificationTokenExpirationMs()));
        verificationToken.setUsed(false);
        user.setEmailVerificationToken(verificationToken);

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        user = userRepository.save(user);
        return user;
    }

    public AuthResponse login(@NotNull LoginRequest request) {
        Optional<User> optionalUser = userRepository.findByEmailAndAudit_DeletedAtIsNull(request.getEmail());
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

        UserInfoRead userInfoRead = userInfoMapper.toRead(info);

        List<RoleRead> roleReads = new ArrayList<>();
        for (Role role : info.getRole()) {
            RoleRead rr = roleMapper.toRead(role);
            ArrayList<GrantRead> grants = new ArrayList<>();
            for (Grant grant : role.getGrants()){
                GrantRead gr = grantMapper.toRead(grant);
                grants.add(gr);
            }
            rr.setGrants(grants);
            roleReads.add(rr);
        }
        userInfoRead.setRoleReads(roleReads.toArray(new RoleRead[0]));

        String accessToken = jwtService.generateToken(info);

        refreshTokenRepository.deleteByUserId(user.get_id().toHexString());

        String tokenValue = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .userId(user.get_id().toHexString())
                .expiryDate(Instant.now().plusMillis(jwtExpiration))
                .build();
        refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse();
        response.setUser(userInfoRead);
        response.setAccessToken(accessToken);
        response.setRefreshToken(tokenValue);
        response.setExpiresIn(jwtExpiration);
        return response;
    }

    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
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

        UserInfoRead userInfoRead = userInfoMapper.toRead(info);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenStr);
        response.setExpiresIn(jwtExpiration);
        response.setUser(userInfoRead);

        return response;
    }

    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Override
    public void verifyEmail(String token) {
        User user = userRepository.getUserByEmailVerificationToken_TokenAndAudit_DeletedAtIsNull(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        VerificationToken verificationToken = user.getEmailVerificationToken();
        if (verificationToken == null) {
            throw new RuntimeException("Invalid verification token");
        }

        if (!verificationToken.isValid()) {
            throw new RuntimeException("Verification token is expired or already used");
        }

        verificationToken.setUsed(true);
        verificationToken.setUsedAt(Instant.now());
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());
        user.setEmailVerificationToken(null);

        userRepository.save(user);
    }

    @Override
    public void forgotPassword(String email) {
        Optional<User> optionalUser = userRepository.findByEmailAndAudit_DeletedAtIsNull(email);
        if (optionalUser.isEmpty()) {
            // Don't reveal if user exists or not for security
            return;
        }

        User user = optionalUser.get();

        VerificationToken resetToken = new VerificationToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiryDate(Instant.now().plusMillis(emailProperties.getPasswordResetTokenExpirationMs()));
        resetToken.setUsed(false);

        user.setPasswordResetToken(resetToken);
        userRepository.save(user);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.getUserByPasswordResetToken_TokenAndAudit_DeletedAtIsNull(token)
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

        VerificationToken resetToken = user.getPasswordResetToken();
        if (resetToken == null) {
            throw new RuntimeException("Invalid password reset token");
        }

        if (!resetToken.isValid()) {
            throw new RuntimeException("Password reset token is expired or already used");
        }

        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);

        userRepository.save(user);
    }
}
