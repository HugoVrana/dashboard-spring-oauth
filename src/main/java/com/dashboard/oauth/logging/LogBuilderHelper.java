package com.dashboard.oauth.logging;

import com.dashboard.oauth.model.log.ApiCallLog;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;

public class LogBuilderHelper {
    public static String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.toString()).append("\n");

        StackTraceElement[] elements = ex.getStackTrace();
        int limit = Math.min(elements.length, 15);

        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(elements[i]).append("\n");
        }

        if (elements.length > limit) {
            sb.append("\t... ").append(elements.length - limit).append(" more");
        }

        return sb.toString();
    }

    public static String getFullUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder(request.getRequestURL());
        String queryString = request.getQueryString();
        if (queryString != null) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public static String getOrCreateRequestId(HttpServletRequest request) {
        Object requestId = request.getAttribute("requestId");
        if (requestId != null) {
            return (String) requestId;
        }
        return UUID.randomUUID().toString();
    }

    public static String getStatusMessage(Integer statusCode) {
        return switch (statusCode / 100) {
            case 2 -> "Success";
            case 3 -> "Redirect";
            case 4 -> "Client Error";
            case 5 -> "Server Error";
            default -> "Unknown";
        };
    }

    public static ApiCallLog.ApiCallLogBuilder buildBaseLog(HttpServletRequest request,
                                                            Integer statusCode,
                                                            Instant timestamp,
                                                            Long durationMs) {
        return ApiCallLog.builder()
                .requestId(getOrCreateRequestId(request))
                .timestamp(timestamp)
                .method(request.getMethod())
                .endpoint(request.getRequestURI())
                .fullUrl(getFullUrl(request))
                .statusCode(statusCode)
                .statusMessage(getStatusMessage(statusCode))
                .clientIp(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .durationMs(durationMs)
                .service("Dashboard API - Spring")
                .environment(System.getProperty("spring.profiles.active", "dev"))
                .version("1.0.0");
    }
}