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
import com.dashboard.oauth.service.interfaces.IEmailTemplateService;
import com.resend.core.exception.ResendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService implements IEmailService {

    private final IUserRepository userRepository;
    private final IEmailSenderService emailSenderService;
    private final IEmailSendAttemptRepository emailSendAttemptRepository;
    private final IEmailTemplateService emailTemplateService;
    private final EmailProperties emailProperties;

    @Override
    public void sendPendingVerificationEmails() {
        List<User> users = userRepository.findUsersWithUnsentVerificationEmail();
        for (User user : users) {
            VerificationToken token = user.getEmailVerificationToken();
            if (token == null) {
                return;
            }

            String subject = "Verify your email";
            String verifyUrl = emailProperties.getBaseUrl() + "/verify-email?token=" + token.getToken();
            long expirationHours = emailProperties.getVerificationTokenExpirationMs() / (1000 * 60 * 60);
            String content = emailTemplateService.renderVerificationEmail(verifyUrl, expirationHours);

            EmailSendAttempt attempt = createAttempt(user, EmailType.VERIFICATION, token.getToken());
            Instant startTime = Instant.now();

            try {
                String messageId = emailSenderService.sendEmail(user.getEmail(), subject, content);
                markAttemptSuccess(attempt, messageId);
                token.setEmailSentAt(Instant.now());
                userRepository.save(user);
                long durationMs = Duration.between(startTime, Instant.now()).toMillis();
                log.info("event=email_send status=SENT emailType=VERIFICATION userId={} email={} tokenId={} messageId={} durationMs={}",
                        user.get_id().toHexString(), user.getEmail(), token.getToken(), messageId, durationMs);
            } catch (ResendException e) {
                markAttemptFailed(attempt, e.getMessage());
                long durationMs = Duration.between(startTime, Instant.now()).toMillis();
                log.error("event=email_send status=FAILED emailType=VERIFICATION userId={} email={} tokenId={} error={} durationMs={}",
                        user.get_id().toHexString(), user.getEmail(), token.getToken(), e.getMessage(), durationMs);
            }

            emailSendAttemptRepository.save(attempt);
        }
    }

    @Override
    public void sendPendingPasswordResetEmails() {
        List<User> users = userRepository.findUsersWithUnsentPasswordResetEmail();
        for (User user : users) {
            VerificationToken token = user.getPasswordResetToken();
            if (token == null) {
                break;
            }

            String subject = "Reset your password";
            String resetUrl = emailProperties.getBaseUrl() + "/reset-password?token=" + token.getToken();
            long expirationHours = emailProperties.getPasswordResetTokenExpirationMs() / (1000 * 60 * 60);
            String content = emailTemplateService.renderPasswordResetEmail(resetUrl, expirationHours);

            EmailSendAttempt attempt = createAttempt(user, EmailType.PASSWORD_RESET, token.getToken());
            Instant startTime = Instant.now();

            try {
                String messageId = emailSenderService.sendEmail(user.getEmail(), subject, content);
                markAttemptSuccess(attempt, messageId);
                token.setEmailSentAt(Instant.now());
                userRepository.save(user);
                long durationMs = Duration.between(startTime, Instant.now()).toMillis();
                log.info("event=email_send status=SENT emailType=PASSWORD_RESET userId={} email={} tokenId={} messageId={} durationMs={}",
                        user.get_id().toHexString(), user.getEmail(), token.getToken(), messageId, durationMs);
            } catch (ResendException e) {
                markAttemptFailed(attempt, e.getMessage());
                long durationMs = Duration.between(startTime, Instant.now()).toMillis();
                log.error("event=email_send status=FAILED emailType=PASSWORD_RESET userId={} email={} tokenId={} error={} durationMs={}",
                        user.get_id().toHexString(), user.getEmail(), token.getToken(), e.getMessage(), durationMs);
            }

            emailSendAttemptRepository.save(attempt);
        }
    }

    private EmailSendAttempt createAttempt(@NotNull User user, EmailType emailType, String tokenId) {
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

    private void markAttemptSuccess(@NotNull EmailSendAttempt attempt, String messageId) {
        attempt.setStatus(EmailSendStatus.SENT);
        attempt.setResendMessageId(messageId);
        attempt.setSentAt(Instant.now());
    }

    private void markAttemptFailed(@NotNull EmailSendAttempt attempt, String errorMessage) {
        attempt.setStatus(EmailSendStatus.FAILED);
        attempt.setErrorMessage(errorMessage);
    }
}
