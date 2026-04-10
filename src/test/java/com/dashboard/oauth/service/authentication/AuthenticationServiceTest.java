package com.dashboard.oauth.service.authentication;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterResponse;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.oauth.RefreshToken;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.model.entities.user.VerificationToken;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Auth Service")
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest extends BaseAuthenticationServiceTest {

    @Test
    @DisplayName("Should return user info when user is registered")
    void register_shouldCreateNewUser() {
        ObjectId roleId = new ObjectId();
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setRoleId(roleId.toHexString());

        Role role = new Role();
        role.set_id(roleId);
        role.setName("TEST_ROLE");
        role.setGrants(new ArrayList<>());

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(roleService.getRoleById(roleId)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.get_id() == null) user.set_id(new ObjectId());
            return user;
        });

        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail("newuser@example.com");
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(new UserInfo());
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        RegisterResponse registerResponse = authenticationService.register(request, null);

        assertNotNull(registerResponse);
        assertNotNull(registerResponse.getUser());
        assertEquals("newuser@example.com", registerResponse.getUser().getEmail());
        assertTrue(registerResponse.isRequiresTwoFactorEnrollment());
        assertEquals("ENROLL_2FA", registerResponse.getNextStep());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user already exists")
    void register_shouldThrowExceptionWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authenticationService.register(request, null)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when password is too short")
    void login_shouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail("test@example.com");

        UserInfo userInfo = new UserInfo();
        userInfo.setRole(new ArrayList<>());

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(userInfo);
        when(jwtService.generateToken(any(UserInfo.class), any(), any())).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        AuthResponse response = authenticationService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(jwtProperties.getExpiration(), response.getExpiresIn());
        verify(refreshTokenRepository).deleteByUserId(any(ObjectId.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should throw exception when user is not found")
    void login_shouldThrowExceptionWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw DisabledException when email is not verified")
    void login_shouldThrowDisabledExceptionWhenEmailNotVerified() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unverified@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        user.setEmailVerified(false);

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("unverified@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> authenticationService.login(request));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should throw exception when password is invalid")
    void login_shouldThrowExceptionWhenPasswordInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        User user = createTestUser();

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.login(request)
        );

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    @DisplayName("Should delete existing refresh token before creating new")
    void login_shouldDeleteExistingRefreshTokenBeforeCreatingNew() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        UserInfoRead userInfoRead = new UserInfoRead();

        UserInfo userInfo = new UserInfo();
        userInfo.setRole(new ArrayList<>());

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(userInfo);
        when(jwtService.generateToken(any(UserInfo.class), any(), any())).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        authenticationService.login(request);

        // Verify delete is called before save
        var inOrder = inOrder(refreshTokenRepository);
        inOrder.verify(refreshTokenRepository).deleteByUserId(user.get_id());
        inOrder.verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should return new access token when refresh token is valid")
    void refreshToken_shouldReturnNewAccessToken() {
        ObjectId refreshTokenId = new ObjectId();
        String refreshTokenStr = refreshTokenId.toHexString();
        User user = createTestUser();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenId)
                .userId(user.get_id())
                .expiryDate(Instant.now().plusMillis(jwtProperties.getExpiration()))
                .build();

        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail("test@example.com");

        when(refreshTokenRepository.findByToken(refreshTokenId)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(user.get_id())).thenReturn(Optional.of(user));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(new UserInfo());
        when(jwtService.generateToken(any(UserInfo.class), any(), any())).thenReturn("new-access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        AuthResponse response = authenticationService.refreshToken(refreshTokenStr);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals(refreshTokenStr, response.getRefreshToken());
        assertEquals(jwtProperties.getExpiration(), response.getExpiresIn());
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void refreshToken_shouldThrowExceptionWhenTokenNotFound() {
        ObjectId refreshTokenId = new ObjectId();
        String refreshTokenStr = refreshTokenId.toHexString();

        when(refreshTokenRepository.findByToken(refreshTokenId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.refreshToken(refreshTokenStr)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when refresh token format is invalid")
    void refreshToken_shouldThrowExceptionWhenTokenFormatInvalid() {
        String invalidTokenStr = "not-a-valid-objectid";

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.refreshToken(invalidTokenStr)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
        verify(refreshTokenRepository, never()).findByToken(any(ObjectId.class));
    }

    @Test
    @DisplayName("Should throw exception when refresh token is expired")
    void refreshToken_shouldThrowExceptionWhenTokenExpired() {
        ObjectId refreshTokenId = new ObjectId();
        String refreshTokenStr = refreshTokenId.toHexString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenId)
                .userId(new ObjectId())
                .expiryDate(Instant.now().minusMillis(1000)) // Expired
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenId)).thenReturn(Optional.of(refreshToken));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.refreshToken(refreshTokenStr)
        );

        assertEquals("Refresh token expired", exception.getMessage());
        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    @DisplayName("Should throw exception when user is not found")
    void refreshToken_shouldThrowExceptionWhenUserNotFound() {
        ObjectId refreshTokenId = new ObjectId();
        String refreshTokenStr = refreshTokenId.toHexString();
        ObjectId userId = new ObjectId();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenId)
                .userId(userId)
                .expiryDate(Instant.now().plusMillis(jwtProperties.getExpiration()))
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenId)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.refreshToken(refreshTokenStr)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    @DisplayName("Should delete refresh token when logout is called")
    void logout_shouldDeleteRefreshTokenByUserId() {
        String token = "test-jwt-token";
        String authHeader = "Bearer " + token;
        String email = "test@example.com";
        ObjectId userId = new ObjectId();

        User user = new User();
        user.set_id(userId);
        user.setEmail(email);

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(userRepository.findByEmailAndAudit_DeletedAtIsNull(email)).thenReturn(Optional.of(user));

        authenticationService.logout(authHeader);

        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    @DisplayName("Should encode password before saving user")
    void register_shouldEncodePassword() {
        ObjectId roleId = new ObjectId();
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("plainPassword");
        request.setRoleId(roleId.toHexString());

        Role role = new Role();
        role.set_id(roleId);
        role.setName("TEST_ROLE");
        role.setGrants(new ArrayList<>());

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleService.getRoleById(roleId)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.get_id() == null) user.set_id(new ObjectId());
            return user;
        });
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(new UserInfo());
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(new UserInfoRead());

        authenticationService.register(request, null);

        verify(passwordEncoder).encode("plainPassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        User savedUser = userCaptor.getAllValues().getFirst();
        assertEquals("$2a$10$encodedPassword", savedUser.getPassword());
    }

    @Test
    @DisplayName("Should save refresh token with correct expiry date")
    void login_shouldSaveRefreshTokenWithCorrectExpiration() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        UserInfoRead userInfoRead = new UserInfoRead();

        UserInfo userInfo = new UserInfo();
        userInfo.setRole(new ArrayList<>());

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(userInfo);
        when(jwtService.generateToken(any(UserInfo.class), any(), any())).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        Instant beforeLogin = Instant.now();
        authenticationService.login(request);
        Instant afterLogin = Instant.now();

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getToken());
        assertEquals(user.get_id(), savedToken.getUserId());

        // Check expiry is roughly JWT_EXPIRATION in the future
        assertTrue(savedToken.getExpiryDate().isAfter(beforeLogin.plusMillis(jwtProperties.getExpiration() - 1000)));
        assertTrue(savedToken.getExpiryDate().isBefore(afterLogin.plusMillis(jwtProperties.getExpiration() + 1000)));
    }

    @Test
    @DisplayName("Should check if user is locked before authentication")
    void login_shouldCheckIfUserIsLocked() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        UserInfo userInfo = new UserInfo();
        userInfo.setRole(new ArrayList<>());

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(userInfo);
        when(jwtService.generateToken(any(UserInfo.class), any(), any())).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(new UserInfoRead());

        authenticationService.login(request);

        verify(loginAttemptService).checkLocked(user);
    }

    @Test
    @DisplayName("Should throw LockedException when user account is locked")
    void login_shouldThrowLockedExceptionWhenUserIsLocked() {
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        user.setLocked(true);

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("locked@example.com"))
                .thenReturn(Optional.of(user));
        doThrow(new LockedException("User account is locked"))
                .when(loginAttemptService).checkLocked(user);

        LockedException exception = assertThrows(
                LockedException.class,
                () -> authenticationService.login(request)
        );

        assertEquals("User account is locked", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should record failed attempt when password is invalid")
    void login_shouldRecordFailedAttemptOnBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        User user = createTestUser();

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(request));

        verify(loginAttemptService).recordFailedAttempt(user);
    }

    @Test
    @DisplayName("Should record successful login after authentication")
    void login_shouldRecordSuccessfulLoginAfterAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        user.setFailedLoginAttempts(3);
        UserInfo userInfo = new UserInfo();
        userInfo.setRole(new ArrayList<>());

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(userInfo);
        when(jwtService.generateToken(any(UserInfo.class), any(), any())).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(new UserInfoRead());

        authenticationService.login(request);

        verify(loginAttemptService).recordSuccessfulLogin(user);
    }

    @Test
    @DisplayName("Should call checkLocked before authentication attempt")
    void login_shouldCheckLockedBeforeAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        UserInfo userInfo = new UserInfo();
        userInfo.setRole(new ArrayList<>());

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(userInfo);
        when(jwtService.generateToken(any(UserInfo.class), any(), any())).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(new UserInfoRead());

        authenticationService.login(request);

        var inOrder = inOrder(loginAttemptService, authenticationManager);
        inOrder.verify(loginAttemptService).checkLocked(user);
        inOrder.verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Should reset failed login attempts and unlock user on password reset")
    void resetPassword_shouldUnlockUserAndResetFailedAttempts() {
        ObjectId tokenId = new ObjectId();
        String token = tokenId.toHexString();

        User user = createTestUser();
        user.setLocked(true);
        user.setFailedLoginAttempts(5);

        VerificationToken resetToken = new VerificationToken();
        resetToken.set_id(tokenId);
        resetToken.setExpiryDate(Instant.now().plusMillis(3600000));
        resetToken.setUsed(false);
        user.setPasswordResetToken(resetToken);

        when(userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(tokenId))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authenticationService.resetPassword(token, "newPassword123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(0, savedUser.getFailedLoginAttempts());
        assertFalse(savedUser.isLocked());
        assertEquals("encodedNewPassword", savedUser.getPassword());
    }

    @Test
    @DisplayName("Should return true when password reset token is valid")
    void validatePasswordResetToken_shouldReturnTrueForValidToken() {
        ObjectId tokenId = new ObjectId();
        String token = tokenId.toHexString();

        User user = createTestUser();
        VerificationToken resetToken = new VerificationToken();
        resetToken.set_id(tokenId);
        resetToken.setExpiryDate(Instant.now().plusMillis(3600000));
        resetToken.setUsed(false);
        user.setPasswordResetToken(resetToken);

        when(userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(tokenId))
                .thenReturn(Optional.of(user));

        boolean result = authenticationService.validatePasswordResetToken(token);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when password reset token format is invalid")
    void validatePasswordResetToken_shouldReturnFalseForInvalidFormat() {
        boolean result = authenticationService.validatePasswordResetToken("invalid-token-format");

        assertFalse(result);
        verify(userRepository, never()).getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(any());
    }

    @Test
    @DisplayName("Should return false when password reset token does not exist")
    void validatePasswordResetToken_shouldReturnFalseForNonExistentToken() {
        ObjectId tokenId = new ObjectId();
        String token = tokenId.toHexString();

        when(userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(tokenId))
                .thenReturn(Optional.empty());

        boolean result = authenticationService.validatePasswordResetToken(token);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when password reset token is expired")
    void validatePasswordResetToken_shouldReturnFalseForExpiredToken() {
        ObjectId tokenId = new ObjectId();
        String token = tokenId.toHexString();

        User user = createTestUser();
        VerificationToken resetToken = new VerificationToken();
        resetToken.set_id(tokenId);
        resetToken.setExpiryDate(Instant.now().minusMillis(1000)); // Expired
        resetToken.setUsed(false);
        user.setPasswordResetToken(resetToken);

        when(userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(tokenId))
                .thenReturn(Optional.of(user));

        boolean result = authenticationService.validatePasswordResetToken(token);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when password reset token is already used")
    void validatePasswordResetToken_shouldReturnFalseForUsedToken() {
        ObjectId tokenId = new ObjectId();
        String token = tokenId.toHexString();

        User user = createTestUser();
        VerificationToken resetToken = new VerificationToken();
        resetToken.set_id(tokenId);
        resetToken.setExpiryDate(Instant.now().plusMillis(3600000));
        resetToken.setUsed(true); // Already used
        user.setPasswordResetToken(resetToken);

        when(userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(tokenId))
                .thenReturn(Optional.of(user));

        boolean result = authenticationService.validatePasswordResetToken(token);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when user has no password reset token")
    void validatePasswordResetToken_shouldReturnFalseWhenUserHasNoToken() {
        ObjectId tokenId = new ObjectId();
        String token = tokenId.toHexString();

        User user = createTestUser();
        user.setPasswordResetToken(null);

        when(userRepository.getUserByPasswordResetToken__idAndAudit_DeletedAtIsNull(tokenId))
                .thenReturn(Optional.of(user));

        boolean result = authenticationService.validatePasswordResetToken(token);

        assertFalse(result);
    }

    private User createTestUser() {
        User user = new User();
        user.set_id(new ObjectId());
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRoles(new ArrayList<>());
        user.setEmailVerified(true);

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }
}
