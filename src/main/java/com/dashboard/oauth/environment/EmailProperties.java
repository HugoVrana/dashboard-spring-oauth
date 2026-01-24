package com.dashboard.oauth.environment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "email")
public class EmailProperties {
    private String baseUrl = "http://localhost:3000";
    private String fromAddress = "Acme <onboarding@resend.dev>";
    private Long verificationTokenExpirationMs = 86400000L; // 24 hours
    private Long passwordResetTokenExpirationMs = 3600000L; // 1 hour
}
