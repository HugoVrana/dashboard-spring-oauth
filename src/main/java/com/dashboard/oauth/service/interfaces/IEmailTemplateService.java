package com.dashboard.oauth.service.interfaces;

public interface IEmailTemplateService {
    String renderVerificationEmail(String verifyUrl, long expirationHours);

    String renderPasswordResetEmail(String resetUrl, long expirationHours);
}
