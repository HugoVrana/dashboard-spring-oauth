package com.dashboard.oauth.exceptionHandlers;

import com.dashboard.oauth.logging.GrafanaHttpClient;
import com.dashboard.oauth.logging.LogBuilderHelper;
import com.dashboard.oauth.model.log.ApiCallLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final GrafanaHttpClient grafanaHttpClient;

    public GlobalExceptionHandler(GrafanaHttpClient grafanaHttpClient) {
        this.grafanaHttpClient = grafanaHttpClient;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        // Optional: aggregate field errors into pd.setProperty("errors", ...)
        pd.setDetail(ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage());
        return pd;
    }

    @ExceptionHandler(ChangeSetPersister.NotFoundException.class)
    ProblemDetail handleNotFound(ChangeSetPersister.NotFoundException ex) {
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Resource not found");
        pd.setDetail(ex.getMessage());
        pd.setType(URI.create("https://example.com/problems/not-found"));
        return pd;
    }

    // Handle all exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred", ex);

        // Log to Grafana
        try {
            Instant timestamp = Instant.now();
            ApiCallLog.ApiCallLogBuilder builder = LogBuilderHelper.buildBaseLog(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), timestamp, null);
            ApiCallLog logEntry = builder
                    .errorMessage(ex.getMessage())
                    .errorType(ex.getClass().getSimpleName())
                    .stackTrace(LogBuilderHelper.getStackTrace(ex)).build();
            grafanaHttpClient.send(logEntry);
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
}