package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.totp.TotpSetupResponse;
import com.dashboard.oauth.model.entities.mfa.TotpConfig;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.service.interfaces.IUserService;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Epic("Authentication")
@Feature("2FA / TOTP")
@Tag("service-totp")
@DisplayName("Totp Service")
@ExtendWith(MockitoExtension.class)
class TotpServiceTest {

    @Mock
    private IUserService userService;

    @InjectMocks
    private TotpService totpService;

    private ObjectId testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = new ObjectId();
        testUser = createTestUser();
    }

    // -------------------------------------------------------------------------
    // setupTotp
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return setup response with QR code and secret when user exists")
    void setupTotp_shouldReturnSetupResponse() {
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userService.saveUser(any())).thenReturn(testUser);

        TotpSetupResponse response = totpService.setupTotp(testUserId.toHexString());

        assertThat(response.getSecret()).isNotBlank();
        assertThat(response.getQrCodeDataUri()).startsWith("data:image/png;base64,");
    }

    @Test
    @DisplayName("Should save user with disabled TotpConfig during setup")
    void setupTotp_shouldSaveUserWithDisabledConfig() {
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userService.saveUser(any())).thenReturn(testUser);

        totpService.setupTotp(testUserId.toHexString());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getTwoFactorConfig()).isInstanceOf(TotpConfig.class);
        assertThat(((TotpConfig) saved.getTwoFactorConfig()).isEnabled()).isFalse();
        assertThat(((TotpConfig) saved.getTwoFactorConfig()).getSecret()).isNotBlank();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found during setup")
    void setupTotp_shouldThrowWhenUserNotFound() {
        when(userService.getUserById(testUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> totpService.setupTotp(testUserId.toHexString()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userService, never()).saveUser(any());
    }

    // -------------------------------------------------------------------------
    // verifyTotp
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true and enable TOTP when first valid code is submitted")
    void verifyTotp_shouldEnableTotpOnFirstValidCode() throws Exception {
        String secret = new DefaultSecretGenerator(32).generate();
        String code = generateCurrentCode(secret);

        testUser.setTwoFactorConfig(buildTotpConfig(secret, false));
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));
        when(userService.saveUser(any())).thenReturn(testUser);

        boolean result = totpService.verifyTotp(testUserId.toHexString(), code);

        assertThat(result).isTrue();
        assertThat(((TotpConfig) testUser.getTwoFactorConfig()).isEnabled()).isTrue();
        verify(userService).saveUser(testUser);
    }

    @Test
    @DisplayName("Should return true but not re-save when TOTP is already enabled")
    void verifyTotp_shouldNotSaveWhenAlreadyEnabled() throws Exception {
        String secret = new DefaultSecretGenerator(32).generate();
        String code = generateCurrentCode(secret);

        testUser.setTwoFactorConfig(buildTotpConfig(secret, true));
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        boolean result = totpService.verifyTotp(testUserId.toHexString(), code);

        assertThat(result).isTrue();
        verify(userService, never()).saveUser(any());
    }

    @Test
    @DisplayName("Should return false and not save when code is invalid")
    void verifyTotp_shouldReturnFalseForInvalidCode() {
        testUser.setTwoFactorConfig(buildTotpConfig("JBSWY3DPEHPK3PXP", false));
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        boolean result = totpService.verifyTotp(testUserId.toHexString(), "000000");

        assertThat(result).isFalse();
        verify(userService, never()).saveUser(any());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when no TOTP config is set")
    void verifyTotp_shouldThrowWhenNoTotpConfig() {
        testUser.setTwoFactorConfig(null);
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> totpService.verifyTotp(testUserId.toHexString(), "123456"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("TOTP not configured");
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when TOTP secret is null")
    void verifyTotp_shouldThrowWhenSecretIsNull() {
        TotpConfig config = new TotpConfig();
        config.setSecret(null);
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        config.setAudit(audit);
        testUser.setTwoFactorConfig(config);
        when(userService.getUserById(testUserId)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> totpService.verifyTotp(testUserId.toHexString(), "123456"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("TOTP not configured");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found during verify")
    void verifyTotp_shouldThrowWhenUserNotFound() {
        when(userService.getUserById(testUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> totpService.verifyTotp(testUserId.toHexString(), "123456"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User createTestUser() {
        User user = new User();
        user.set_id(testUserId);
        user.setEmail("test@example.com");
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);
        return user;
    }

    private TotpConfig buildTotpConfig(String secret, boolean enabled) {
        TotpConfig config = new TotpConfig();
        config.setSecret(secret);
        config.setEnabled(enabled);
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        config.setAudit(audit);
        return config;
    }

    private String generateCurrentCode(String secret) throws Exception {
        DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        long bucket = Math.floorDiv(Instant.now().getEpochSecond(), 30);
        return generator.generate(secret, bucket);
    }
}
