package com.dashboard.oauth.controller.v2;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.oauth.config.RsaKeyPair;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.environment.OidcProperties;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.repository.IServerKeyRepository;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("OIDC Discovery")
@Feature("Well-Known")
@Tag("controller-well-known")
@WebMvcTest(WellKnownController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestConfig.class, WellKnownControllerTest.WellKnownTestConfig.class})
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
class WellKnownControllerTest {

    private static final String ISSUER = "http://localhost";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private GrafanaHttpClient grafanaHttpClient;

    @MockitoBean
    private IActivityFeedService activityFeedService;

    @TestConfiguration
    static class WellKnownTestConfig {

        @Bean
        public OidcProperties oidcProperties() {
            OidcProperties props = new OidcProperties();
            props.setIssuer(ISSUER);
            return props;
        }

        @Bean
        public RsaKeyPair rsaKeyPair() throws Exception {
            IServerKeyRepository repo = mock(IServerKeyRepository.class);
            when(repo.findAll()).thenReturn(List.of());
            return new RsaKeyPair(repo);
        }
    }

    // -------------------------------------------------------------------------
    // GET /.well-known/openid-configuration
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /.well-known/openid-configuration → 200 with all required OIDC fields")
    void openidConfiguration_returnsRequiredFields() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(ISSUER))
                .andExpect(jsonPath("$.authorization_endpoint").value(ISSUER + "/api/v2/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value(ISSUER + "/api/v2/oauth2/token"))
                .andExpect(jsonPath("$.userinfo_endpoint").value(ISSUER + "/api/v2/oauth2/userinfo"))
                .andExpect(jsonPath("$.jwks_uri").value(ISSUER + "/.well-known/jwks.json"));
    }

    @Test
    @DisplayName("GET /.well-known/openid-configuration → includes supported response types, scopes, and algorithms")
    void openidConfiguration_returnsCapabilities() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_types_supported[0]").value("code"))
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported[0]").value("RS256"))
                .andExpect(jsonPath("$.scopes_supported").isArray())
                .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"));
    }

    // -------------------------------------------------------------------------
    // GET /.well-known/jwks.json
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /.well-known/jwks.json → 200 with RSA public key")
    void jwks_returnsRsaKey() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].e").isNotEmpty());
    }
}
