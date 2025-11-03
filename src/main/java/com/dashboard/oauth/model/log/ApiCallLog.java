package com.dashboard.oauth.model.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ApiCallLog {
    // Request Information
    private String requestId;           // Unique identifier for tracing
    private String method;              // GET, POST, PUT, DELETE, etc.
    private String endpoint;            // /api/users/{id}
    private String fullUrl;             // Full request URL with query params

    // Timing Information
    private Instant timestamp;          // When the request started
    private Long durationMs;            // How long the request took

    // Response Information
    private Integer statusCode;         // HTTP status code (200, 404, 500, etc.)
    private String statusMessage;       // OK, Not Found, Internal Server Error

    // Client Information
    private String clientIp;            // Client IP address
    private String userAgent;           // Browser/client info
    private String userId;              // If authenticated, user identifier

    // Error Information (optional)
    private String errorMessage;        // Error message if request failed
    private String errorType;           // Exception class name
    private String stackTrace;          // Stack trace for errors (be careful with PII)

    // Additional Context
    private Map<String, String> headers;        // Important headers (filtered)
    private Map<String, Object> requestBody;    // Request payload (sanitized)
    private Map<String, Object> responseBody;   // Response payload (sanitized)
    private Map<String, String> customFields;   // Any custom metadata

    // Performance Metrics
    private Long requestSize;           // Request body size in bytes
    private Long responseSize;          // Response body size in bytes

    // Business Context
    private String service;             // Service name
    private String environment;         // dev, staging, prod
    private String version;             // API version

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "ApiCallLog{error serializing to JSON: " + e.getMessage() + "}";
        }
    }
}
