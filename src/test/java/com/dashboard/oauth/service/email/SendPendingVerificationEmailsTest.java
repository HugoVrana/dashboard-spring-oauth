package com.dashboard.oauth.service.email;

import com.dashboard.oauth.model.entities.EmailSendAttempt;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.enums.EmailSendStatus;
import com.dashboard.oauth.model.enums.EmailType;
import com.resend.core.exception.ResendException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@DisplayName("Send Pending Verification Emails")
public class SendPendingVerificationEmailsTest extends BaseEmailServiceTest {

    @Test
    @DisplayName("Should send email and update token when user has unsent verification email")
    void shouldSendEmailAndUpdateToken() throws ResendException {
        testUser.setEmailVerificationToken(testToken);
        String messageId = "msg_" + UUID.randomUUID();

        when(userRepository.findUsersWithUnsentVerificationEmail())
                .thenReturn(List.of(testUser));
        when(emailSenderService.sendEmail(eq(testEmail), anyString(), anyString()))
                .thenReturn(messageId);

        emailService.sendPendingVerificationEmails();

        verify(emailSenderService).sendEmail(eq(testEmail), eq("Verify your email"), anyString());
        verify(userRepository).save(testUser);
        verify(emailSendAttemptRepository).save(any(EmailSendAttempt.class));

        assertThat(testToken.getEmailSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create attempt with correct data on success")
    void shouldCreateAttemptWithCorrectDataOnSuccess() throws ResendException {
        testUser.setEmailVerificationToken(testToken);
        String messageId = "msg_" + UUID.randomUUID();

        when(userRepository.findUsersWithUnsentVerificationEmail())
                .thenReturn(List.of(testUser));
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(messageId);

        emailService.sendPendingVerificationEmails();

        ArgumentCaptor<EmailSendAttempt> attemptCaptor = ArgumentCaptor.forClass(EmailSendAttempt.class);
        verify(emailSendAttemptRepository).save(attemptCaptor.capture());

        EmailSendAttempt savedAttempt = attemptCaptor.getValue();
        assertThat(savedAttempt.getUserId()).isEqualTo(testUserId);
        assertThat(savedAttempt.getEmailType()).isEqualTo(EmailType.VERIFICATION);
        assertThat(savedAttempt.getRecipientEmail()).isEqualTo(testEmail);
        assertThat(savedAttempt.getStatus()).isEqualTo(EmailSendStatus.SENT);
        assertThat(savedAttempt.getResendMessageId()).isEqualTo(messageId);
        assertThat(savedAttempt.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Should mark attempt as failed when email sending fails")
    void shouldMarkAttemptAsFailedWhenSendingFails() throws ResendException {
        testUser.setEmailVerificationToken(testToken);
        String errorMessage = "Resend API error";

        when(userRepository.findUsersWithUnsentVerificationEmail())
                .thenReturn(List.of(testUser));
        when(emailSenderService.sendEmail(anyString(), anyString(), anyString()))
                .thenThrow(new ResendException(errorMessage));

        emailService.sendPendingVerificationEmails();

        ArgumentCaptor<EmailSendAttempt> attemptCaptor = ArgumentCaptor.forClass(EmailSendAttempt.class);
        verify(emailSendAttemptRepository).save(attemptCaptor.capture());

        EmailSendAttempt savedAttempt = attemptCaptor.getValue();
        assertThat(savedAttempt.getStatus()).isEqualTo(EmailSendStatus.FAILED);
        assertThat(savedAttempt.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(savedAttempt.getSentAt()).isNull();

        // Token should not be updated on failure
        assertThat(testToken.getEmailSentAt()).isNull();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should not send email when no users have unsent verification emails")
    void shouldNotSendEmailWhenNoUsers() throws ResendException {
        when(userRepository.findUsersWithUnsentVerificationEmail())
                .thenReturn(Collections.emptyList());

        emailService.sendPendingVerificationEmails();

        verify(emailSenderService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(emailSendAttemptRepository, never()).save(any(EmailSendAttempt.class));
    }

    @Test
    @DisplayName("Should skip user when verification token is null")
    void shouldSkipUserWhenTokenIsNull() throws ResendException {
        testUser.setEmailVerificationToken(null);

        when(userRepository.findUsersWithUnsentVerificationEmail())
                .thenReturn(List.of(testUser));

        emailService.sendPendingVerificationEmails();

        verify(emailSenderService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(emailSendAttemptRepository, never()).save(any(EmailSendAttempt.class));
    }

    @Test
    @DisplayName("Should process multiple users independently")
    void shouldProcessMultipleUsersIndependently() throws ResendException {
        User user1 = createTestUser();
        user1.set_id(new ObjectId());
        user1.setEmail("user1@example.com");
        user1.setEmailVerificationToken(createTestToken());

        User user2 = createTestUser();
        user2.set_id(new ObjectId());
        user2.setEmail("user2@example.com");
        user2.setEmailVerificationToken(createTestToken());

        when(userRepository.findUsersWithUnsentVerificationEmail())
                .thenReturn(List.of(user1, user2));
        when(emailSenderService.sendEmail(eq("user1@example.com"), anyString(), anyString()))
                .thenReturn("msg_1");
        when(emailSenderService.sendEmail(eq("user2@example.com"), anyString(), anyString()))
                .thenThrow(new ResendException("Failed for user2"));

        emailService.sendPendingVerificationEmails();

        // Both attempts should be saved
        verify(emailSendAttemptRepository, times(2)).save(any(EmailSendAttempt.class));

        // Only user1 should be saved (successful)
        verify(userRepository, times(1)).save(user1);
        verify(userRepository, never()).save(user2);

        // user1 token should be updated, user2 should not
        assertThat(user1.getEmailVerificationToken().getEmailSentAt()).isNotNull();
        assertThat(user2.getEmailVerificationToken().getEmailSentAt()).isNull();
    }
}
