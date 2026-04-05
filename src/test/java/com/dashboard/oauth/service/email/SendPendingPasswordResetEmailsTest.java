package com.dashboard.oauth.service.email;

import com.dashboard.oauth.model.entities.email.EmailSendAttempt;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.model.enums.EmailSendStatus;
import com.dashboard.oauth.model.enums.EmailType;
import com.resend.core.exception.ResendException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Send Pending Password Reset Emails")
public class SendPendingPasswordResetEmailsTest extends BaseEmailServiceTest {

    @Test
    @DisplayName("Should send email and update token when user has unsent password reset email")
    void shouldSendEmailAndUpdateToken() throws ResendException {
        testUser.setPasswordResetToken(testToken);
        String messageId = "msg_" + UUID.randomUUID();

        when(userRepository.findUsersWithUnsentPasswordResetEmail())
                .thenReturn(List.of(testUser));
        when(emailSenderService.sendEmail(eq(testEmail), anyString(), anyString()))
                .thenReturn(messageId);

        emailService.sendPendingPasswordResetEmails();

        verify(emailSenderService).sendEmail(eq(testEmail), eq("Reset your password"), anyString());
        verify(userRepository).save(testUser);
        verify(emailSendAttemptRepository).save(any(EmailSendAttempt.class));

        assertThat(testToken.getEmailSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create attempt with PASSWORD_RESET type")
    void shouldCreateAttemptWithPasswordResetType() throws ResendException {
        testUser.setPasswordResetToken(testToken);
        String messageId = "msg_" + UUID.randomUUID();

        when(userRepository.findUsersWithUnsentPasswordResetEmail())
                .thenReturn(List.of(testUser));
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(messageId);

        emailService.sendPendingPasswordResetEmails();

        ArgumentCaptor<EmailSendAttempt> attemptCaptor = ArgumentCaptor.forClass(EmailSendAttempt.class);
        verify(emailSendAttemptRepository).save(attemptCaptor.capture());

        EmailSendAttempt savedAttempt = attemptCaptor.getValue();
        assertThat(savedAttempt.getEmailType()).isEqualTo(EmailType.PASSWORD_RESET);
        assertThat(savedAttempt.getStatus()).isEqualTo(EmailSendStatus.SENT);
    }

    @Test
    @DisplayName("Should mark attempt as failed when email sending fails")
    void shouldMarkAttemptAsFailedWhenSendingFails() throws ResendException {
        testUser.setPasswordResetToken(testToken);
        String errorMessage = "Connection timeout";

        when(userRepository.findUsersWithUnsentPasswordResetEmail())
                .thenReturn(List.of(testUser));
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString()))
                .thenThrow(new ResendException(errorMessage));

        emailService.sendPendingPasswordResetEmails();

        ArgumentCaptor<EmailSendAttempt> attemptCaptor = ArgumentCaptor.forClass(EmailSendAttempt.class);
        verify(emailSendAttemptRepository).save(attemptCaptor.capture());

        EmailSendAttempt savedAttempt = attemptCaptor.getValue();
        assertThat(savedAttempt.getStatus()).isEqualTo(EmailSendStatus.FAILED);
        assertThat(savedAttempt.getErrorMessage()).isEqualTo(errorMessage);

        assertThat(testToken.getEmailSentAt()).isNull();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should skip user when password reset token is null")
    void shouldSkipUserWhenTokenIsNull() throws ResendException {
        testUser.setPasswordResetToken(null);

        when(userRepository.findUsersWithUnsentPasswordResetEmail())
                .thenReturn(List.of(testUser));

        emailService.sendPendingPasswordResetEmails();

        verify(emailSenderService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(emailSendAttemptRepository, never()).save(any(EmailSendAttempt.class));
    }
}
