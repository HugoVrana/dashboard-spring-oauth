package com.dashboard.oauth.service.interfaces;

public interface IEmailService {
    void sendPendingVerificationEmails();
    void sendPendingPasswordResetEmails();
}
