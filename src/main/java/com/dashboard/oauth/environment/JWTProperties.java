package com.dashboard.oauth.environment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JWTProperties {

    private String secret;

    private Long expiration;
}
