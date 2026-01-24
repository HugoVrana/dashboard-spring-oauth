package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.environment.EmailProperties;
import com.dashboard.oauth.model.entities.EmailSendAttempt;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.model.entities.VerificationToken;
import com.dashboard.oauth.model.enums.EmailSendStatus;
import com.dashboard.oauth.model.enums.EmailType;
import com.dashboard.oauth.repository.IEmailSendAttemptRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.service.interfaces.IEmailSenderService;
import com.dashboard.oauth.service.interfaces.IEmailService;
import com.resend.core.exception.ResendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService implements IEmailService {

    private final IUserRepository userRepository;
    private final IEmailSenderService emailSenderService;
    private final IEmailSendAttemptRepository emailSendAttemptRepository;
    private final EmailProperties emailProperties;

    @Override
    public void sendPendingVerificationEmails() {
        List<User> users = userRepository.findUsersWithUnsentVerificationEmail();
        for (User user : users) {
            sendVerificationEmail(user);
        }
    }

    @Override
    public void sendPendingPasswordResetEmails() {
        List<User> users = userRepository.findUsersWithUnsentPasswordResetEmail();
        for (User user : users) {
            sendPasswordResetEmail(user);
        }
    }

    private void sendVerificationEmail(User user) {
        VerificationToken token = user.getEmailVerificationToken();
        if (token == null) {
            return;
        }

        String subject = "Verify your email";
        String content = buildVerificationEmailContent(token.getToken());

        EmailSendAttempt attempt = createAttempt(user, EmailType.VERIFICATION, token.getToken());

        try {
            String messageId = emailSenderService.sendEmail(user.getEmail(), subject, content);
            markAttemptSuccess(attempt, messageId);
            token.setEmailSentAt(Instant.now());
            userRepository.save(user);
            log.info("Verification email sent to {} with messageId {}", user.getEmail(), messageId);
        } catch (ResendException e) {
            markAttemptFailed(attempt, e.getMessage());
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }

        emailSendAttemptRepository.save(attempt);
    }

    private void sendPasswordResetEmail(User user) {
        VerificationToken token = user.getPasswordResetToken();
        if (token == null) {
            return;
        }

        String subject = "Reset your password";
        String content = buildPasswordResetEmailContent(token.getToken());

        EmailSendAttempt attempt = createAttempt(user, EmailType.PASSWORD_RESET, token.getToken());

        try {
            String messageId = emailSenderService.sendEmail(user.getEmail(), subject, content);
            markAttemptSuccess(attempt, messageId);
            token.setEmailSentAt(Instant.now());
            userRepository.save(user);
            log.info("Password reset email sent to {} with messageId {}", user.getEmail(), messageId);
        } catch (ResendException e) {
            markAttemptFailed(attempt, e.getMessage());
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }

        emailSendAttemptRepository.save(attempt);
    }

    private EmailSendAttempt createAttempt(User user, EmailType emailType, String tokenId) {
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());

        return EmailSendAttempt.builder()
                .userId(user.get_id())
                .emailType(emailType)
                .tokenId(tokenId)
                .recipientEmail(user.getEmail())
                .attemptedAt(Instant.now())
                .status(EmailSendStatus.QUEUED)
                .audit(audit)
                .build();
    }

    private void markAttemptSuccess(EmailSendAttempt attempt, String messageId) {
        attempt.setStatus(EmailSendStatus.SENT);
        attempt.setResendMessageId(messageId);
        attempt.setSentAt(Instant.now());
    }

    private void markAttemptFailed(EmailSendAttempt attempt, String errorMessage) {
        attempt.setStatus(EmailSendStatus.FAILED);
        attempt.setErrorMessage(errorMessage);
    }

    private String buildVerificationEmailContent(String token) {
        String verifyUrl = emailProperties.getBaseUrl() + "/verify-email?token=" + token;
        return "<p>Please verify your email by clicking the link below:</p>" +
                "<p><a href=\"" + verifyUrl + "\">Verify Email</a></p>";
    }

    private String buildPasswordResetEmailContent(String token) {
        String resetUrl = emailProperties.getBaseUrl() + "/reset-password?token=" + token;
        return "<p>Click the link below to reset your password:</p>" +
                "<p><a href=\"" + resetUrl + "\">Reset Password</a></p>";
    }
}
