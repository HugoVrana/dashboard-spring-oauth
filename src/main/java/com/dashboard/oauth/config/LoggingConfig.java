package com.dashboard.oauth.config;

import com.dashboard.common.environment.GrafanaProperties;
import com.dashboard.common.logging.GrafanaHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(GrafanaProperties.class)
@RequiredArgsConstructor
public class LoggingConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    public GrafanaHttpClient grafanaHttpClient(GrafanaProperties grafanaProperties, HttpClient httpClient) {
        return new GrafanaHttpClient(grafanaProperties, httpClient);
    }
}