package com.dashboard.oauth.scheduler;

import com.dashboard.oauth.service.interfaces.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailScheduler {

    private final IEmailService emailService;

    @Scheduled(cron = "0 * * * * *")
    public void processVerificationEmails() {
        log.debug("Running verification email scheduler");
        emailService.sendPendingVerificationEmails();
    }

    @Scheduled(cron = "0 * * * * *")
    public void processPasswordResetEmails() {
        log.debug("Running password reset email scheduler");
        emailService.sendPendingPasswordResetEmails();
    }
}
