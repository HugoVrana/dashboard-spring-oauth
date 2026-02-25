package com.dashboard.oauth.scheduler;

import com.dashboard.oauth.service.interfaces.IEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;

@DisplayName("Email Scheduler")
@ExtendWith(MockitoExtension.class)
class EmailSchedulerTest {

    @Mock
    private IEmailService emailService;

    @InjectMocks
    private EmailScheduler emailScheduler;

    @Test
    @DisplayName("Should call sendPendingVerificationEmails when processVerificationEmails runs")
    void processVerificationEmailsShouldCallService() {
        emailScheduler.processVerificationEmails();

        verify(emailService).sendPendingVerificationEmails();
    }

    @Test
    @DisplayName("Should call sendPendingPasswordResetEmails when processPasswordResetEmails runs")
    void processPasswordResetEmailsShouldCallService() {
        emailScheduler.processPasswordResetEmails();

        verify(emailService).sendPendingPasswordResetEmails();
    }
}
