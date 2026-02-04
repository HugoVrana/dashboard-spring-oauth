package com.dashboard.oauth.service.interfaces;

public interface IEmailTemplateService {
    /**
     * Renders the email verification template.
     *
     * @param verifyUrl       the URL for email verification
     * @param expirationHours hours until the link expires
     * @return rendered HTML content
     */
    String renderVerificationEmail(String verifyUrl, long expirationHours);

    /**
     * Renders the password reset template.
     *
     * @param resetUrl        the URL for password reset
     * @param expirationHours hours until the link expires
     * @return rendered HTML content
     */
    String renderPasswordResetEmail(String resetUrl, long expirationHours);
}
