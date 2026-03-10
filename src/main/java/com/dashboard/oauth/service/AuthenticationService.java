package com.dashboard.oauth.service;

import com.dashboard.common.model.ActivityEvent;
import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.NotFoundException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.common.utility.diff.DiffComparer;
import com.dashboard.common.utility.diff.DiffResult;
import com.dashboard.oauth.authentication.GrantsAuthentication;
import com.dashboard.oauth.context.DiffContext;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.role.AddRoleRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.environment.JWTProperties;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.RefreshToken;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.entities.VerificationToken;
import com.dashboard.oauth.model.enums.ActivityEventType;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IJwtService;
import com.dashboard.oauth.service.interfaces.ILoginAttemptService;
import com.dashboard.oauth.service.interfaces.IRoleService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthenticationService {

    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final IRoleService roleService;
    private final ILoginAttemptService loginAttemptService;
    private final IJwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final IUserInfoMapper userInfoMapper;
    private final EmailProperties emailProperties;
    private final JWTProperties jwtProperties;
    private final IActivityFeedService activityFeedService;

    @Override
    public UserInfoRead register(@NotNull RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User with this email already exists");
        }

        if (!ObjectId.isValid(request.getRoleId())) {
            throw new NotFoundException("Role id is invalid.");
        }

        ObjectId roleId = new ObjectId(request.getRoleId());
        Role role = roleService.getRoleById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role with id " + roleId + " not found"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new ArrayList<>());
        user.setEmailVerified(false);

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.set_id(new ObjectId());
        verificationToken.setCreatedAt(Instant.now());
        verificationToken.setExpiryDate(Instant.now().plusMillis(emailProperties.getVerificationTokenExpirationMs()));
        verificationToken.setUsed(false);
        user.setEmailVerificationToken(verificationToken);

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        user = userRepository.save(user);

        user.getRoles().add(role);
        user = userRepository.save(user);

        DiffComparer<User> comparer = new DiffComparer<>(null, user);
        DiffResult diff = comparer.compare();
        DiffContext.addDiff(diff.toJson());
        publishActivityEvent(ActivityEventType.USER_REGISTERED, user);

        UserInfo userInfo = userInfoMapper.toUserInfo(user);
        return userInfoMapper.toRead(userInfo);
    }

    @Override
    public AuthResponse login(@NotNull LoginRequest request) {
        User user = userRepository.findByEmailAndAudit_DeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        loginAttemptService.checkLocked(user);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailedAttempt(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        loginAttemptService.recordSuccessfulLogin(user);

        UserInfo userInfo = userInfoMapper.toUserInfo(user);
        UserInfoRead userInfoRead = userInfoMapper.toRead(userInfo);
        String accessToken = jwtService.generateToken(userInfo);

        refreshTokenRepository.deleteByUserId(user.get_id().toHexString());

        ObjectId refreshTokenId = new ObjectId();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenId)
                .userId(user.get_id().toHexString())
                .expiryDate(Instant.now().plusMillis(jwtProperties.getExpiration()))
                .build();
        refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse();
        response.setUser(userInfoRead);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenId.toHexString());
        response.setExpiresIn(jwtProperties.getExpiration());

        publishActivityEvent(ActivityEventType.USER_LOGGED_IN, user);

        return response;
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenStr) {
        if (!ObjectId.isValid(refreshTokenStr)) {
            throw new RuntimeException("Invalid refresh token");
        }
        RefreshToken refreshToken = refreshTokenRepository.findByToken(new ObjectId(refreshTokenStr))
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        User user = userRepository.findById(new ObjectId(refreshToken.getUserId()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserInfo info = userInfoMapper.toUserInfo(user);
        String accessToken = jwtService.generateToken(info);

        UserInfoRead userInfoRead = userInfoMapper.toRead(info);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenStr);
        response.setExpiresIn(jwtProperties.getExpiration());
        response.setUser(userInfoRead);

        publishActivityEvent(ActivityEventType.TOKEN_REFRESHED, user);

        return response;
    }

    @Override
    public void logout(String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmailAndAudit_DeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenRepository.deleteByUserId(user.get_id().toHexString());

        publishActivityEvent(ActivityEventType.USER_LOGGED_OUT, user);
    }

    @Override
    public AuthResponse addUserRole(AddRoleRequest request) {
        if (!ObjectId.isValid(request.getUserId())) {
            throw new InvalidRequestException("User id is invalid.");
        }
        ObjectId userId = new ObjectId(request.getUserId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User oldState = copyUserState(user);

        if (!ObjectId.isValid(request.getRoleId())) {
            throw new InvalidRequestException("Role id is invalid.");
        }
        ObjectId roleId = new ObjectId(request.getRoleId());
        Role role = roleService.getRoleById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (user.getRoles().contains(role)) {
            throw new ConflictException("User already has this role");
        }

        user.getRoles().add(role);
        user = userRepository.save(user);

        DiffComparer<User> comparer = new DiffComparer<>(oldState, user);
        DiffResult diff = comparer.compare();
        DiffContext.addDiff(diff.toJson());
        publishActivityEvent(ActivityEventType.ROLE_ADDED_TO_USER, user);

        UserInfo userInfo = userInfoMapper.toUserInfo(user);
        UserInfoRead userInfoRead = userInfoMapper.toRead(userInfo);

        AuthResponse response = new AuthResponse();
        response.setUser(userInfoRead);

        return response;
    }

    @Override
    public void verifyEmail(String token) {
        if (!ObjectId.isValid(token)) {
            throw new RuntimeException("Invalid verification token");
        }
        User user = userRepository.getUserByEmailVerificationToken__idAndAudit_DeletedAtIsNull(new ObjectId(token))
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        User oldState = copyUserState(user);

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

        DiffComparer<User> comparer = new DiffComparer<>(oldState, user);
        DiffResult diff = comparer.compare();
        DiffContext.addDiff(diff.toJson());
        publishActivityEvent(ActivityEventType.EMAIL_VERIFIED, user);
    }

    @Override
    public void forgotPassword(String email) {
        Optional<User> optionalUser = userRepository.findByEmailAndAudit_DeletedAtIsNull(email);
        if (optionalUser.isEmpty()) {
            // Don't reveal if user exists or not for security
            return;
        }

        User user = optionalUser.get();
        User oldState = copyUserState(user);

        VerificationToken resetToken = new VerificationToken();
        resetToken.set_id(new ObjectId());
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiryDate(Instant.now().plusMillis(emailProperties.getPasswordResetTokenExpirationMs()));
        resetToken.setUsed(false);

        user.setPasswordResetToken(resetToken);
        userRepository.save(user);

        DiffComparer<User> comparer = new DiffComparer<>(oldState, user);
        DiffResult diff = comparer.compare();
        DiffContext.addDiff(diff.toJson());
        publishActivityEvent(ActivityEventType.PASSWORD_RESET_REQUESTED, user);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        if (!ObjectId.isValid(token)) {
            throw new RuntimeException("Invalid password reset token");
        }
        User user = userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(new ObjectId(token))
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

        User oldState = copyUserState(user);

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

        // Unlock the user account and reset failed login attempts
        user.setFailedLoginAttempts(0);
        user.setLocked(false);

        userRepository.save(user);

        DiffComparer<User> comparer = new DiffComparer<>(oldState, user);
        DiffResult diff = comparer.compare();
        DiffContext.addDiff(diff.toJson());
        publishActivityEvent(ActivityEventType.PASSWORD_RESET, user);
    }

    @Override
    public boolean validatePasswordResetToken(String token) {
        if (!ObjectId.isValid(token)) {
            return false;
        }

        Optional<User> optionalUser = userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(new ObjectId(token));
        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();
        VerificationToken resetToken = user.getPasswordResetToken();
        if (resetToken == null) {
            return false;
        }

        return resetToken.isValid();
    }

    @Override
    public UserInfoRead getCurrentUser(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();
        UserInfo userInfo = userInfoMapper.toUserInfo(user);
        UserInfoRead userInfoRead = userInfoMapper.toRead(userInfo);
        return userInfoRead;
    }

    private void publishActivityEvent(ActivityEventType type, User user) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", user.get_id().toHexString());
        metadata.put("userEmail", user.getEmail());

        // Try to get authenticated user info, but don't fail if not authenticated
        String actorId = user.get_id().toHexString();
        String actorImageUrl = "";
        try {
            GrantsAuthentication auth = GrantsAuthentication.current();
            actorId = auth.getUserId();
            actorImageUrl = auth.getProfileImageUrlOrEmpty();
        } catch (IllegalStateException ignored) {
            // Not authenticated - use target user as actor (self-service action)
        }
        metadata.put("userImageUrl", actorImageUrl);

        ActivityEvent event = ActivityEvent.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .type(type.name())
                .actorId(actorId)
                .metadata(metadata)
                .build();
        activityFeedService.publishEvent(event);
    }

    private User copyUserState(User user) {
        User copy = new User();
        copy.set_id(user.get_id());
        copy.setEmail(user.getEmail());
        copy.setEmailVerified(user.getEmailVerified());
        copy.setRoles(new ArrayList<>(user.getRoles()));
        copy.setLocked(user.getLocked());
        copy.setFailedLoginAttempts(user.getFailedLoginAttempts());
        return copy;
    }
}
