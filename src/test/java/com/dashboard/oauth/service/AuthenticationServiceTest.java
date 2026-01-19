package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.RefreshToken;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IJwtService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IJwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private IUserInfoMapper userInfoMapper;

    @InjectMocks
    private AuthenticationService authenticationService;

    private static final Long JWT_EXPIRATION = 86400000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "jwtExpiration", JWT_EXPIRATION);
    }

    @Test
    void register_shouldCreateNewUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setRoleId(new ObjectId().toHexString());

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.set_id(new ObjectId());
            return user;
        });

        User result = authenticationService.register(request);

        assertNotNull(result);
        assertEquals("newuser@example.com", result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        assertNotNull(result.getAudit());
        assertNotNull(result.getAudit().getCreatedAt());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowExceptionWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        AuthenticationServiceException exception = assertThrows(
                AuthenticationServiceException.class,
                () -> authenticationService.register(request)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_shouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail("test@example.com");

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
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
    void login_shouldDeleteExistingRefreshTokenBeforeCreatingNew() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        UserInfoRead userInfoRead = new UserInfoRead();

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserInfo.class))).thenReturn("access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        authenticationService.login(request);

        // Verify delete is called before save
        var inOrder = inOrder(refreshTokenRepository);
        inOrder.verify(refreshTokenRepository).deleteByUserId(user.get_id().toHexString());
        inOrder.verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_shouldReturnNewAccessToken() {
        String refreshTokenStr = "valid-refresh-token";
        User user = createTestUser();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(user.get_id().toHexString())
                .expiryDate(Instant.now().plusMillis(JWT_EXPIRATION))
                .build();

        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail("test@example.com");

        when(refreshTokenRepository.findByToken(refreshTokenStr)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(user.get_id())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserInfo.class))).thenReturn("new-access-token");
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        AuthResponse response = authenticationService.refreshToken(refreshTokenStr);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals(refreshTokenStr, response.getRefreshToken());
        assertEquals(JWT_EXPIRATION, response.getExpiresIn());
    }

    @Test
    void refreshToken_shouldThrowExceptionWhenTokenNotFound() {
        String refreshTokenStr = "invalid-refresh-token";

        when(refreshTokenRepository.findByToken(refreshTokenStr)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.refreshToken(refreshTokenStr)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void refreshToken_shouldThrowExceptionWhenTokenExpired() {
        String refreshTokenStr = "expired-refresh-token";
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(new ObjectId().toHexString())
                .expiryDate(Instant.now().minusMillis(1000)) // Expired
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenStr)).thenReturn(Optional.of(refreshToken));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.refreshToken(refreshTokenStr)
        );

        assertEquals("Refresh token expired", exception.getMessage());
        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void refreshToken_shouldThrowExceptionWhenUserNotFound() {
        String refreshTokenStr = "valid-refresh-token";
        ObjectId userId = new ObjectId();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(userId.toHexString())
                .expiryDate(Instant.now().plusMillis(JWT_EXPIRATION))
                .build();

        when(refreshTokenRepository.findByToken(refreshTokenStr)).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authenticationService.refreshToken(refreshTokenStr)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void logout_shouldDeleteRefreshTokenByUserId() {
        String userId = new ObjectId().toHexString();

        authenticationService.logout(userId);

        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    void register_shouldEncodePassword() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("plainPassword");
        request.setRoleId(new ObjectId().toHexString());

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authenticationService.register(request);

        assertEquals("$2a$10$encodedPassword", result.getPassword());
        verify(passwordEncoder).encode("plainPassword");
    }

    @Test
    void login_shouldSaveRefreshTokenWithCorrectExpiration() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = createTestUser();
        UserInfoRead userInfoRead = new UserInfoRead();

        when(userRepository.findByEmailAndAudit_DeletedAtIsNull("test@example.com"))
                .thenReturn(Optional.of(user));
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
