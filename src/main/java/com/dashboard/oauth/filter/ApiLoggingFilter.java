package com.dashboard.oauth.filter;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.logging.LogBuilderHelper;
import com.dashboard.common.model.log.ApiCallLog;
import com.dashboard.oauth.context.DiffContext;
import com.dashboard.oauth.logging.RequestLoggingAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final String SERVICE_NAME = "spring-dashboard-oauth";
    private static final int REQUEST_CACHE_LIMIT = 1024 * 1024;

    private final ObjectMapper objectMapper;
    private final GrafanaHttpClient grafanaHttpClient;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getAttribute(RequestLoggingAttributes.REQUEST_START_TIME) == null) {
            request.setAttribute(RequestLoggingAttributes.REQUEST_START_TIME, Instant.now());
        }
        if (request.getAttribute(RequestLoggingAttributes.REQUEST_ID) == null) {
            request.setAttribute(RequestLoggingAttributes.REQUEST_ID, UUID.randomUUID().toString());
        }

        ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper cachingRequest
                ? cachingRequest
                : new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
        ContentCachingResponseWrapper wrappedResponse = response instanceof ContentCachingResponseWrapper cachingResponse
                ? cachingResponse
                : new ContentCachingResponseWrapper(response);

        boolean requestFailed = false;
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (ServletException | IOException | RuntimeException ex) {
            requestFailed = true;
            annotateException(request, ex);
            throw ex;
        } finally {
            try {
                sendLog(wrappedRequest, wrappedResponse);
            } catch (Exception loggingEx) {
                log.error("Failed to log API call", loggingEx);
            } finally {
                DiffContext.clear();
                if (!requestFailed) {
                    wrappedResponse.copyBodyToResponse();
                }
            }
        }
    }

    private void sendLog(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        Instant startTime = (Instant) request.getAttribute(RequestLoggingAttributes.REQUEST_START_TIME);
        Instant timestamp = startTime != null ? startTime : Instant.now();
        Long durationMs = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : null;
        Exception exception = (Exception) request.getAttribute(RequestLoggingAttributes.EXCEPTION);

        LogBuilderHelper logBuilderHelper = new LogBuilderHelper(objectMapper);
        ApiCallLog.ApiCallLogBuilder builder = logBuilderHelper.buildBaseLog(
                SERVICE_NAME,
                request,
                response,
                timestamp,
                durationMs
        );

        if (exception != null) {
            builder.errorMessage(exception.getMessage());
            builder.errorType(exception.getClass().getName());
            builder.stackTrace(logBuilderHelper.getStackTrace(exception));
        }

        Map<String, String> customFields = new HashMap<>();
        String diff = DiffContext.getDiff();
        if (diff != null) {
            customFields.put("diff", diff);
        }
        if (!customFields.isEmpty()) {
            builder.customFields(customFields);
        }

        grafanaHttpClient.send(builder.build());
    }

    private void annotateException(HttpServletRequest request, Exception ex) {
        request.setAttribute(RequestLoggingAttributes.EXCEPTION, ex);
    }
}
