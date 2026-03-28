package com.dashboard.oauth.filter;

import com.dashboard.common.environment.GrafanaProperties;
import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.log.ApiCallLog;
import com.dashboard.oauth.logging.RequestLoggingAttributes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiLoggingFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TestGrafanaHttpClient grafanaHttpClient = new TestGrafanaHttpClient();
    private final ApiLoggingFilter filter = new ApiLoggingFilter(objectMapper, grafanaHttpClient);

    @Test
    @DisplayName("Should log successful responses to Grafana")
    void shouldLogSuccessfulResponses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContentType("application/json");
        request.setContent("{\"email\":\"user@example.com\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {
                res.setStatus(201);
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":\"created\"}");
            }
        };
        MockFilterChain chain = new MockFilterChain(servlet);

        filter.doFilter(request, response, chain);

        ApiCallLog log = grafanaHttpClient.lastLog;
        assertEquals("POST", log.getMethod());
        assertEquals("/api/test", log.getEndpoint());
        assertEquals(201, log.getStatusCode());
        assertEquals("Success", log.getStatusMessage());
        assertNotNull(log.getRequestId());
        assertNull(log.getErrorMessage());
        assertNull(log.getErrorType());
        assertEquals("created", log.getResponseBody().get("status"));
    }

    @Test
    @DisplayName("Should log handled exceptions to Grafana")
    void shouldLogHandledExceptions() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RuntimeException exception = new RuntimeException("boom");

        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {
                req.setAttribute(RequestLoggingAttributes.EXCEPTION, exception);
                res.setStatus(500);
                res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"boom\"}");
            }
        };
        MockFilterChain chain = new MockFilterChain(servlet);

        filter.doFilter(request, response, chain);

        ApiCallLog log = grafanaHttpClient.lastLog;
        assertEquals(500, log.getStatusCode());
        assertEquals("boom", log.getErrorMessage());
        assertEquals(RuntimeException.class.getName(), log.getErrorType());
        assertNotNull(log.getStackTrace());
        assertEquals("boom", log.getResponseBody().get("message"));
    }

    private static final class TestGrafanaHttpClient extends GrafanaHttpClient {
        private ApiCallLog lastLog;

        private TestGrafanaHttpClient() {
            super(new GrafanaProperties(), HttpClient.newHttpClient());
        }

        @Override
        public void send(ApiCallLog apiCallLog) {
            this.lastLog = apiCallLog;
        }
    }
}
