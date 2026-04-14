package com.dashboard.oauth.integration.v2;

import com.dashboard.oauth.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Well-Known / OIDC Discovery Endpoints")
@TestPropertySource(properties = "oidc.issuer=http://localhost")
class WellKnownControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /.well-known/openid-configuration returns OIDC discovery document")
    void openidConfiguration_returnsDocument() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost"))
                .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost/api/v2/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value("http://localhost/api/v2/oauth2/token"))
                .andExpect(jsonPath("$.jwks_uri").value("http://localhost/.well-known/jwks.json"))
                .andExpect(jsonPath("$.response_types_supported[0]").value("code"))
                .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"))
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported[0]").value("RS256"));
    }

    @Test
    @DisplayName("GET /.well-known/jwks.json returns JWKS with RSA public key")
    void jwks_returnsPublicKey() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").exists());
    }
}
