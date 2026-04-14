package com.dashboard.oauth.controller.v2;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.authentication.GrantsAuthentication;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.dataTransferObject.totp.TotpSetupResponse;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.ITotpService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Authentication")
@Feature("TOTP Controller")
@Tag("controller-totp")
@WebMvcTest(TotpController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
class TotpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ITotpService totpService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private GrafanaHttpClient grafanaHttpClient;

    @MockitoBean
    private IActivityFeedService activityFeedService;

    private GrantsAuthentication auth;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = new ObjectId().toHexString();
        auth = new GrantsAuthentication("user@example.com", userId, null, List.of());
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/auth/2fa/setup
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v2/auth/2fa/setup → 200 with qrCodeDataUri and secret")
    void setup_returnsSetupResponse() throws Exception {
        TotpSetupResponse response = new TotpSetupResponse("data:image/png;base64,abc", "JBSWY3DPEHPK3PXP");
        when(totpService.setupTotp(userId)).thenReturn(response);

        mockMvc.perform(post("/api/v2/auth/2fa/setup").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCodeDataUri").value("data:image/png;base64,abc"))
                .andExpect(jsonPath("$.secret").value("JBSWY3DPEHPK3PXP"));
    }

    @Test
    @DisplayName("POST /api/v2/auth/2fa/setup → 404 when user not found")
    void setup_returns404WhenUserNotFound() throws Exception {
        when(totpService.setupTotp(userId)).thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/v2/auth/2fa/setup").principal(auth))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/auth/2fa/verify
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v2/auth/2fa/verify with valid code → 200")
    void verify_validCode_returns200() throws Exception {
        when(totpService.verifyTotp(userId, "123456")).thenReturn(true);

        mockMvc.perform(post("/api/v2/auth/2fa/verify")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v2/auth/2fa/verify with invalid code → 400")
    void verify_invalidCode_returns400() throws Exception {
        when(totpService.verifyTotp(userId, "000000")).thenReturn(false);

        mockMvc.perform(post("/api/v2/auth/2fa/verify")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v2/auth/2fa/verify with 5-digit code → 400 validation error")
    void verify_fiveDigitCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v2/auth/2fa/verify")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"12345\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v2/auth/2fa/verify with blank code → 400 validation error")
    void verify_blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v2/auth/2fa/verify")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v2/auth/2fa/verify → 400 when TOTP not configured")
    void verify_totpNotConfigured_returns400() throws Exception {
        when(totpService.verifyTotp(userId, "123456"))
                .thenThrow(new InvalidRequestException("TOTP not configured for this user"));

        mockMvc.perform(post("/api/v2/auth/2fa/verify")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest());
    }
}
