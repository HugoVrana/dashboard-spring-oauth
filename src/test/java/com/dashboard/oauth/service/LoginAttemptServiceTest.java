package com.dashboard.oauth.service;

import com.dashboard.oauth.environment.LoginProperties;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IUserRepository;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.LockedException;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Epic("Authentication")
@Feature("Login Attempt Service")
@Tag("service-login-attempt")
@DisplayName("Login Attempt Service")
@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private IUserRepository userRepository;

    private LoginAttemptService loginAttemptService;
    private LoginProperties loginProperties;

    @BeforeEach
    void setUp() {
        loginProperties = new LoginProperties();
        loginProperties.setMaxFailedAttempts(5);
        loginAttemptService = new LoginAttemptService(userRepository, loginProperties);
    }

    @Test
    @DisplayName("Should throw LockedException when user is locked")
    void checkLocked_shouldThrowWhenUserIsLocked() {
        User user = createTestUser();
        user.setLocked(true);

        LockedException exception = assertThrows(
                LockedException.class,
                () -> loginAttemptService.checkLocked(user)
        );

        assertEquals("User account is locked", exception.getMessage());
    }

    @Test
    @DisplayName("Should not throw when user is not locked")
    void checkLocked_shouldNotThrowWhenUserIsNotLocked() {
        User user = createTestUser();
        user.setLocked(false);

        assertDoesNotThrow(() -> loginAttemptService.checkLocked(user));
    }

    @Test
    @DisplayName("Should not throw when locked is null")
    void checkLocked_shouldNotThrowWhenLockedIsNull() {
        User user = createTestUser();
        user.setLocked(null);

        assertDoesNotThrow(() -> loginAttemptService.checkLocked(user));
    }

    @Test
    @DisplayName("Should increment failed login attempts")
    void recordFailedAttempt_shouldIncrementFailedAttempts() {
        User user = createTestUser();
        user.setFailedLoginAttempts(0);

        loginAttemptService.recordFailedAttempt(user);

        verify(userRepository).incrementFailedLoginAttempts(eq(user.get_id()), any(Instant.class));
    }

    @Test
    @DisplayName("Should lock user when max attempts reached")
    void recordFailedAttempt_shouldLockUserWhenMaxAttemptsReached() {
        User user = createTestUser();
        user.setFailedLoginAttempts(4); // Next attempt will be 5th

        loginAttemptService.recordFailedAttempt(user);

        verify(userRepository).incrementFailedLoginAttempts(eq(user.get_id()), any(Instant.class));
        verify(userRepository).lockUser(eq(user.get_id()), any(Instant.class));
    }

    @Test
    @DisplayName("Should not lock user when below max attempts")
    void recordFailedAttempt_shouldNotLockUserWhenBelowMaxAttempts() {
        User user = createTestUser();
        user.setFailedLoginAttempts(2);

        loginAttemptService.recordFailedAttempt(user);

        verify(userRepository).incrementFailedLoginAttempts(eq(user.get_id()), any(Instant.class));
        verify(userRepository, never()).lockUser(any(), any());
    }

    @Test
    @DisplayName("Should handle null failed attempts as zero")
    void recordFailedAttempt_shouldHandleNullFailedAttemptsAsZero() {
        User user = createTestUser();
        user.setFailedLoginAttempts(null);

        loginAttemptService.recordFailedAttempt(user);

        verify(userRepository).incrementFailedLoginAttempts(eq(user.get_id()), any(Instant.class));
        verify(userRepository, never()).lockUser(any(), any());
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void recordSuccessfulLogin_shouldResetFailedAttempts() {
        User user = createTestUser();
        user.setFailedLoginAttempts(3);

        loginAttemptService.recordSuccessfulLogin(user);

        verify(userRepository).resetFailedLoginAttempts(user.get_id());
    }

    @Test
    @DisplayName("Should not reset when failed attempts is zero")
    void recordSuccessfulLogin_shouldNotResetWhenZeroAttempts() {
        User user = createTestUser();
        user.setFailedLoginAttempts(0);

        loginAttemptService.recordSuccessfulLogin(user);

        verify(userRepository, never()).resetFailedLoginAttempts(any());
    }

    @Test
    @DisplayName("Should not reset when failed attempts is null")
    void recordSuccessfulLogin_shouldNotResetWhenNullAttempts() {
        User user = createTestUser();
        user.setFailedLoginAttempts(null);

        loginAttemptService.recordSuccessfulLogin(user);

        verify(userRepository, never()).resetFailedLoginAttempts(any());
    }

    @Test
    @DisplayName("Should lock user on exactly max attempts")
    void recordFailedAttempt_shouldLockOnExactlyMaxAttempts() {
        loginProperties.setMaxFailedAttempts(3);
        User user = createTestUser();
        user.setFailedLoginAttempts(2); // Next will be 3rd (max)

        loginAttemptService.recordFailedAttempt(user);

        verify(userRepository).lockUser(eq(user.get_id()), any(Instant.class));
    }

    @Test
    @DisplayName("Should lock user when already over max attempts")
    void recordFailedAttempt_shouldLockWhenOverMaxAttempts() {
        loginProperties.setMaxFailedAttempts(3);
        User user = createTestUser();
        user.setFailedLoginAttempts(5); // Already over max

        loginAttemptService.recordFailedAttempt(user);

        verify(userRepository).lockUser(eq(user.get_id()), any(Instant.class));
    }

    private User createTestUser() {
        User user = new User();
        user.set_id(new ObjectId());
        user.setEmail("test@example.com");
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        return user;
    }
}
