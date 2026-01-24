package com.dashboard.oauth.config;

import com.dashboard.oauth.environment.ResendProperties;
import com.resend.Resend;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ResendConfig {

    private final ResendProperties resendProperties;

    @Bean
    public Resend resend() {
        return new Resend(resendProperties.getApiKey());
    }
}
