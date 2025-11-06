package com.dashboard.oauth.interceptor;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.logging.LogBuilderHelper;
import com.dashboard.common.model.log.ApiCallLog;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiLoggingInterceptor implements HandlerInterceptor {

    private static final String REQUEST_START_TIME = "requestStartTime";
    private static final String REQUEST_ID = "requestId";

    private final ObjectMapper objectMapper;
    private final GrafanaHttpClient grafanaHttpClient;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        request.setAttribute(REQUEST_START_TIME, Instant.now());
        request.setAttribute(REQUEST_ID, UUID.randomUUID().toString());
        return true;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                                @NotNull Object handler, Exception ex) {
        if (ex == null) {
            try {
                ApiCallLog log = captureApiCall(request, response);
                grafanaHttpClient.send(log);
            } catch (Exception e) {
                log.error("Failed to log API call", e);
            }
        }
    }

    private ApiCallLog captureApiCall(HttpServletRequest request, HttpServletResponse response) {
        Instant startTime = (Instant) request.getAttribute(REQUEST_START_TIME);
        Instant endTime = Instant.now();
        Long durationMs = startTime != null ?
                java.time.Duration.between(startTime, endTime).toMillis() : null;

        Instant timestamp = startTime != null ? startTime : Instant.now();
        ApiCallLog.ApiCallLogBuilder builder = LogBuilderHelper.buildBaseLog(
                "spring-dashboard",
                request,
                response.getStatus(),
                timestamp,
                durationMs
        );

        builder.userId(extractUserId(request))
                .headers(extractHeaders(request))
                .level(determineLogLevel(response.getStatus()));

        if (request instanceof ContentCachingRequestWrapper) {
            builder.requestBody(extractRequestBody((ContentCachingRequestWrapper) request))
                    .requestSize(getRequestSize((ContentCachingRequestWrapper) request));
        }

        if (response instanceof ContentCachingResponseWrapper) {
            builder.responseBody(extractResponseBody((ContentCachingResponseWrapper) response))
                    .responseSize(getResponseSize((ContentCachingResponseWrapper) response));
        }

        return builder.build();
    }

    private String determineLogLevel(int statusCode) {
        if (statusCode >= 500) return "error";
        if (statusCode >= 400) return "warn";
        return "info";
    }

    private String extractUserId(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        return null;
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!isSensitiveHeader(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }

        return headers;
    }

    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.equals("authorization") ||
                lower.equals("cookie") ||
                lower.equals("set-cookie") ||
                lower.equals("x-api-key");
    }

    private Map<String, Object> extractRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String characterEncoding = request.getCharacterEncoding();
                String requestBodyAsString = new String(content, characterEncoding);
                return objectMapper.readValue(requestBodyAsString, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.debug("Could not parse request body", e);
        }
        return null;
    }

    private Map<String, Object> extractResponseBody(ContentCachingResponseWrapper response) {
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content, response.getCharacterEncoding());
                return objectMapper.readValue(body, new TypeReference<>(){});
            }
        } catch (Exception e) {
            log.debug("Could not parse response body", e);
        }
        return null;
    }

    private Long getRequestSize(ContentCachingRequestWrapper request) {
        return (long) request.getContentAsByteArray().length;
    }

    private Long getResponseSize(ContentCachingResponseWrapper response) {
        return (long) response.getContentAsByteArray().length;
    }
}