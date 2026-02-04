package com.dashboard.oauth.service;

import com.dashboard.oauth.service.interfaces.IEmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailTemplateService implements IEmailTemplateService {

    private final TemplateEngine templateEngine;

    @Override
    public String renderVerificationEmail(String verifyUrl, long expirationHours) {
        Context context = new Context();
        context.setVariable("verifyUrl", verifyUrl);
        context.setVariable("expirationHours", expirationHours);
        return templateEngine.process("email/verification-email", context);
    }

    @Override
    public String renderPasswordResetEmail(String resetUrl, long expirationHours) {
        Context context = new Context();
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("expirationHours", expirationHours);
        return templateEngine.process("email/password-reset-email", context);
    }
}
