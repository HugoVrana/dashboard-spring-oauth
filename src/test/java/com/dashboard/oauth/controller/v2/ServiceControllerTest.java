package com.dashboard.oauth.controller.v2;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.dataTransferObject.grant.EnsureGrantsResponse;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Service V2")
@Feature("Service-to-service API")
@Tag("v2-controller-service")
@WebMvcTest(ServiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
@WithMockUser
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IGrantService grantService;

    @MockitoBean
    private IOAuthClientService oAuthClientService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private UserDetailsService springUserDetailsService;

    @MockitoBean
    private GrafanaHttpClient grafanaHttpClient;

    private String validBasicAuth;
    private List<GrantCreate> grants;

    @BeforeEach
    void setUp() {
        validBasicAuth = "Basic " + Base64.getEncoder()
                .encodeToString("service:secret".getBytes(StandardCharsets.UTF_8));

        GrantCreate g1 = new GrantCreate();
        g1.setName("data-api-resource-read");
        g1.setDescription("Read resources");

        GrantCreate g2 = new GrantCreate();
        g2.setName("data-api-resource-write");
        g2.setDescription("Write resources");

        grants = List.of(g1, g2);
    }

    @Test
    @DisplayName("POST /v2/service/grants/ensure → 200 with created and alreadyExisted lists")
    void ensureGrants_shouldReturn200_whenAuthorized() throws Exception {
        EnsureGrantsResponse response = new EnsureGrantsResponse(
                List.of("data-api-resource-read"),
                List.of("data-api-resource-write")
        );

        when(oAuthClientService.validateClientCredentials(validBasicAuth)).thenReturn(true);
        when(grantService.ensureGrants(any())).thenReturn(response);

        mockMvc.perform(post("/v2/service/grants/ensure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", validBasicAuth)
                        .content(objectMapper.writeValueAsString(grants)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created[0]").value("data-api-resource-read"))
                .andExpect(jsonPath("$.alreadyExisted[0]").value("data-api-resource-write"));
    }

    @Test
    @DisplayName("POST /v2/service/grants/ensure → 401 when service secret is invalid")
    void ensureGrants_shouldReturn401_whenSecretInvalid() throws Exception {
        when(oAuthClientService.validateClientCredentials(any())).thenReturn(false);

        mockMvc.perform(post("/v2/service/grants/ensure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Basic invalid")
                        .content(objectMapper.writeValueAsString(grants)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v2/service/grants/ensure → 401 when Authorization header is missing")
    void ensureGrants_shouldReturn401_whenNoAuthHeader() throws Exception {
        when(oAuthClientService.validateClientCredentials(null)).thenReturn(false);

        mockMvc.perform(post("/v2/service/grants/ensure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grants)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v2/service/grants/ensure → 400 when grant name is blank")
    void ensureGrants_shouldReturn400_whenGrantNameBlank() throws Exception {
        when(oAuthClientService.validateClientCredentials(validBasicAuth)).thenReturn(true);

        GrantCreate invalid = new GrantCreate();
        invalid.setName("");

        mockMvc.perform(post("/v2/service/grants/ensure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", validBasicAuth)
                        .content(objectMapper.writeValueAsString(List.of(invalid))))
                .andExpect(status().isBadRequest());
    }
}
