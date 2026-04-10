package com.dashboard.oauth.exceptionhandlers;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.logging.LogBuilderHelper;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.NotFoundException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.common.model.log.ApiCallLog;
import com.dashboard.oauth.logging.RequestLoggingAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String REQUEST_START_TIME = "requestStartTime";
    private final GrafanaHttpClient grafanaHttpClient;
    private final ObjectMapper objectMapper;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        annotateException(request, ex);
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        // Optional: aggregate field errors into pd.setProperty("errors", ...)
        pd.setDetail(ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage());
        return pd;
    }

    @ExceptionHandler(ChangeSetPersister.NotFoundException.class)
    ProblemDetail handleNotFound(ChangeSetPersister.NotFoundException ex, HttpServletRequest request) {
        annotateException(request, ex);
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Resource not found");
        pd.setDetail(ex.getMessage());
        pd.setType(URI.create("https://example.com/problems/not-found"));
        return pd;
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<String> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        annotateException(request, ex);
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
        annotateException(request, ex);
        var pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Conflict");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        annotateException(request, ex);
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Resource not found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundCommon(NotFoundException ex, HttpServletRequest request) {
        annotateException(request, ex);
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Not found");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex, HttpServletRequest request) {
        annotateException(request, ex);
        var pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Account disabled");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        annotateException(request, ex);
        var pd = ProblemDetail.forStatus(ex.getStatusCode());
        pd.setTitle(ex.getReason());
        pd.setDetail(ex.getMessage());
        return pd;
    }

    // Handle all exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred", ex);
        annotateException(request, ex);

        // Log to Grafana
        try {
            Instant startTime = (Instant) request.getAttribute(REQUEST_START_TIME);
            Instant endTime = Instant.now();
            Long durationMs = startTime != null ?
                    java.time.Duration.between(startTime, endTime).toMillis() : null;

            Instant timestamp = Instant.now();
            LogBuilderHelper logBuilderHelper = new LogBuilderHelper(objectMapper);
            ApiCallLog.ApiCallLogBuilder builder = logBuilderHelper.buildBaseLog(
                    "spring-dashboard-oauth",
                    request,
                    null,
                    timestamp,
                    durationMs);

            ApiCallLog log = builder.build();
            grafanaHttpClient.send(log);
        } catch (Exception loggingEx) {
            log.error("Failed to log exception to Grafana", loggingEx);
        }
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    private void annotateException(HttpServletRequest request, Exception ex) {
        request.setAttribute(RequestLoggingAttributes.EXCEPTION, ex);
    }
}
