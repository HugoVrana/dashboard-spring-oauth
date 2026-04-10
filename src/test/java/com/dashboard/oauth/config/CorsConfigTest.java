package com.dashboard.oauth.config;

import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("CorsConfig")
@ExtendWith(MockitoExtension.class)
class CorsConfigTest {

    @Mock
    private IOAuthClientService oAuthClientService;

    @Mock
    private HttpServletRequest request;

    private CorsConfigurationSource source;

    private static final String ALLOWED_ORIGIN = "https://app.example.com";
    private static final String CLIENT_ID = "test-client";

    @BeforeEach
    void setUp() {
        source = new CorsConfig(oAuthClientService).corsConfigurationSource();
    }

    // -------------------------------------------------------------------------
    // No Origin header
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Returns null when Origin header is missing")
    void returnsNull_whenNoOriginHeader() {
        when(request.getHeader("Origin")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        assertNull(source.getCorsConfiguration(request));
    }

    @Test
    @DisplayName("Returns null when Origin header is blank")
    void returnsNull_whenOriginHeaderIsBlank() {
        when(request.getHeader("Origin")).thenReturn("  ");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        assertNull(source.getCorsConfiguration(request));
    }

    // -------------------------------------------------------------------------
    // /api/v1/auth/** — per-client origin check via X-Client-Id
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("/api/v1/auth — returns config when origin is allowed for client")
    void v1Auth_returnsConfig_whenOriginAllowed() {
        when(request.getHeader("Origin")).thenReturn(ALLOWED_ORIGIN);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Client-Id")).thenReturn(CLIENT_ID);
        when(oAuthClientService.isAllowedHost(eq(CLIENT_ID), any())).thenReturn(true);

        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains(ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("/api/v1/auth — returns null when origin is blocked for client")
    void v1Auth_returnsNull_whenOriginBlocked() {
        when(request.getHeader("Origin")).thenReturn(ALLOWED_ORIGIN);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Client-Id")).thenReturn(CLIENT_ID);
        when(oAuthClientService.isAllowedHost(eq(CLIENT_ID), any())).thenReturn(false);

        assertNull(source.getCorsConfiguration(request));
    }

    // -------------------------------------------------------------------------
    // /api/v2/oauth2/authorize — per-client origin check via client_id param
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("/api/v2/oauth2/authorize — returns config when origin is allowed for client")
    void v2Authorize_returnsConfig_whenOriginAllowed() {
        when(request.getHeader("Origin")).thenReturn(ALLOWED_ORIGIN);
        when(request.getRequestURI()).thenReturn("/api/v2/oauth2/authorize");
        when(request.getParameter("client_id")).thenReturn(CLIENT_ID);
        when(oAuthClientService.isAllowedHost(eq(CLIENT_ID), any())).thenReturn(true);

        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains(ALLOWED_ORIGIN));
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
    }

    @Test
    @DisplayName("/api/v2/oauth2/authorize — returns null when origin is blocked for client")
    void v2Authorize_returnsNull_whenOriginBlocked() {
        when(request.getHeader("Origin")).thenReturn(ALLOWED_ORIGIN);
        when(request.getRequestURI()).thenReturn("/api/v2/oauth2/authorize");
        when(request.getParameter("client_id")).thenReturn(CLIENT_ID);
        when(oAuthClientService.isAllowedHost(eq(CLIENT_ID), any())).thenReturn(false);

        assertNull(source.getCorsConfiguration(request));
    }

    // -------------------------------------------------------------------------
    // /api/** — default origins
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("/api/v2/user — returns config with default origins")
    void apiPath_returnsDefaultOrigins() {
        when(request.getHeader("Origin")).thenReturn(ALLOWED_ORIGIN);
        when(request.getRequestURI()).thenReturn("/api/v2/user/");

        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3001"));
    }

    // -------------------------------------------------------------------------
    // Unknown paths
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Returns null for non-api paths")
    void returnsNull_forUnknownPath() {
        when(request.getHeader("Origin")).thenReturn(ALLOWED_ORIGIN);
        when(request.getRequestURI()).thenReturn("/some-other-path");

        assertNull(source.getCorsConfiguration(request));
    }
}
