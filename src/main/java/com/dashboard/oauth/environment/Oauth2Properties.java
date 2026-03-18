package com.dashboard.oauth.environment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.security.oauth2")
public final class Oauth2Properties {
    private String secret;

    /** URL of the React login page, e.g. https://app.example.com/login */
    private String loginUrl;
}
