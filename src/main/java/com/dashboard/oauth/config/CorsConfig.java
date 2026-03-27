package com.dashboard.oauth.config;

import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private static final List<String> DEFAULT_API_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:3001"
    );

    private final IOAuthClientService oAuthClientService;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return request -> {
            String path = request.getRequestURI();
            String origin = request.getHeader("Origin");
            if (origin == null || origin.isBlank()) {
                return null;
            }

            if (path.startsWith("/api/v1/auth")) {
                String clientId = request.getHeader("X-Client-Id");
                if (!oAuthClientService.isAllowedHost(clientId, request)) {
                    return null;
                }
                return buildConfiguration(List.of(origin), List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
            }

            if (path.startsWith("/v2/oauth2/authorize")) {
                String clientId = request.getParameter("client_id");
                if (!oAuthClientService.isAllowedHost(clientId, request)) {
                    return null;
                }
                return buildConfiguration(List.of(origin), List.of("GET", "POST", "OPTIONS"));
            }

            if (path.startsWith("/api/")) {
                return buildConfiguration(DEFAULT_API_ORIGINS, List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
            }

            return null;
        };
    }

    private CorsConfiguration buildConfiguration(List<String> allowedOrigins, List<String> allowedMethods) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.addAllowedHeader(CorsConfiguration.ALL);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        return configuration;
    }
}
