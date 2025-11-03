package com.dashboard.oauth.logging;

import com.dashboard.oauth.environment.GrafanaProperties;
import com.dashboard.oauth.model.log.ApiCallLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class GrafanaHttpClient {

    private final GrafanaProperties environment;

    public GrafanaHttpClient(GrafanaProperties environment) {
        this.environment = environment;
    }

    public void send(ApiCallLog apiCallLog) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Instant instant = Instant.now();
            String timestampNanos = String.valueOf(instant.getEpochSecond() * 1_000_000_000L + instant.getNano());
            Map<String, Object> payload = Map.of(
                    "streams", List.of(
                            Map.of(
                                    "stream", Map.of(
                                            "service", "spring-dashboard",
                                            "environment", "dev",
                                            "level", "INFO"
                                    ),
                                    "values", List.of(
                                            List.of(timestampNanos, apiCallLog.toString())
                                    )
                            )
                    )
            );
            String body = mapper.writeValueAsString(payload);
            System.out.println("Sending to Grafana: " + body);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(environment.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + environment.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
