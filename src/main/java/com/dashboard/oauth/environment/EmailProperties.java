package com.dashboard.oauth.environment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "email")
public final class EmailProperties {
    private String baseUrl;
    private String fromAddress;
    private Long verificationTokenExpirationMs; // 24 hours
    private Long passwordResetTokenExpirationMs; // 1 hour
}
