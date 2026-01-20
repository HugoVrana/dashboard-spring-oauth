package com.dashboard.oauth.interceptor;
import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.logging.LogBuilderHelper;
import com.dashboard.common.model.log.ApiCallLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Instant;
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
                Instant startTime = (Instant) request.getAttribute(REQUEST_START_TIME);
                Instant endTime = Instant.now();
                Long durationMs = startTime != null ?
                        java.time.Duration.between(startTime, endTime).toMillis() : null;

                Instant timestamp = startTime != null ? startTime : Instant.now();
                LogBuilderHelper logBuilderHelper = new LogBuilderHelper(objectMapper);
                ApiCallLog.ApiCallLogBuilder builder = logBuilderHelper.buildBaseLog(
                        "spring-dashboard-oauth",
                        request,
                        response,
                        timestamp,
                        durationMs
                );

                ApiCallLog log = builder.build();
                grafanaHttpClient.send(log);
            } catch (Exception e) {
                log.error("Failed to log API call", e);
            }
        }
    }
}