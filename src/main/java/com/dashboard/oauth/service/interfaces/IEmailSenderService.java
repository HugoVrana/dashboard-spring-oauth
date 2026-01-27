package com.dashboard.oauth.service.interfaces;

import com.resend.core.exception.ResendException;

public interface IEmailSenderService {
    /**
     * Sends an email via Resend.
     *
     * @return the Resend message ID for tracking
     * @throws ResendException if sending fails
     */
    String sendEmail(String to, String subject, String content) throws ResendException;
}
