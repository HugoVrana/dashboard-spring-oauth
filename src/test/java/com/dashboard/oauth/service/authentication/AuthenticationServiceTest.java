package com.dashboard.oauth.service.authentication;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.NotFoundException;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.RefreshToken;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
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

        UserInfoRead result = authenticationService.register(request);

        assertNotNull(result);
        assertEquals("newuser@example.com", result.getEmail());
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
                () -> authenticationService.register(request)
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
        when(jwtService.generateToken(any(UserInfo.class))).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        AuthResponse response = authenticationService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(JWT_EXPIRATION, response.getExpiresIn());
        verify(refreshTokenRepository).deleteByUserId(anyString());
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
        when(jwtService.generateToken(any(UserInfo.class))).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        authenticationService.login(request);

        // Verify delete is called before save
        var inOrder = inOrder(refreshTokenRepository);
        inOrder.verify(refreshTokenRepository).deleteByUserId(user.get_id().toHexString());
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
                .userId(user.get_id().toHexString())
                .expiryDate(Instant.now().plusMillis(JWT_EXPIRATION))
                .build();

        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail("test@example.com");

        when(refreshTokenRepository.findByToken(refreshTokenId)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(user.get_id())).thenReturn(Optional.of(user));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(new UserInfo());
        when(jwtService.generateToken(any(UserInfo.class))).thenReturn("new-access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        AuthResponse response = authenticationService.refreshToken(refreshTokenStr);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals(refreshTokenStr, response.getRefreshToken());
        assertEquals(JWT_EXPIRATION, response.getExpiresIn());
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
                .userId(new ObjectId().toHexString())
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
                .userId(userId.toHexString())
                .expiryDate(Instant.now().plusMillis(JWT_EXPIRATION))
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
        String userId = new ObjectId().toHexString();

        authenticationService.logout(userId);

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
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(new UserInfo());
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(new UserInfoRead());

        authenticationService.register(request);

        verify(passwordEncoder).encode("plainPassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        User savedUser = userCaptor.getAllValues().get(0);
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
        when(jwtService.generateToken(any(UserInfo.class))).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        Instant beforeLogin = Instant.now();
        authenticationService.login(request);
        Instant afterLogin = Instant.now();

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getToken());
        assertEquals(user.get_id().toHexString(), savedToken.getUserId());

        // Check expiry is roughly JWT_EXPIRATION in the future
        assertTrue(savedToken.getExpiryDate().isAfter(beforeLogin.plusMillis(JWT_EXPIRATION - 1000)));
        assertTrue(savedToken.getExpiryDate().isBefore(afterLogin.plusMillis(JWT_EXPIRATION + 1000)));
    }

    private User createTestUser() {
        User user = new User();
        user.set_id(new ObjectId());
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRoles(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }
}
