package com.dashboard.oauth.environment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class GrafanaProperties {
    @Value("${GRAFANA_API_KEY}")
    private String apiKey;

    @Value("${GRAFANA_LOG_PUSH_API_URL}")
    private String url;
}