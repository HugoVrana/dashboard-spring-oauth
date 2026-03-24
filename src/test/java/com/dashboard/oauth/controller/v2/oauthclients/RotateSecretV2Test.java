package com.dashboard.oauth.controller.v2.oauthclients;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /v2/oauthclients/{id}/secret")
class RotateSecretV2Test extends BaseV2OAuthClientControllerTest {

    @Test
    @DisplayName("Should return 200 with new secret")
    void shouldReturn200_withNewSecret() throws Exception {
        when(oAuthClientService.rotateSecret(any())).thenReturn(testClientCreated);

        mockMvc.perform(post("/v2/oauthclients/{id}/secret", testClientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testClientId))
                .andExpect(jsonPath("$.clientSecret").value(testClientSecret));
    }

    @Test
    @DisplayName("Should return 404 when client not found")
    void shouldReturn404_whenClientNotFound() throws Exception {
        when(oAuthClientService.rotateSecret(any()))
                .thenThrow(new ResourceNotFoundException("OAuth client not found"));

        mockMvc.perform(post("/v2/oauthclients/{id}/secret", testClientId))
                .andExpect(status().isNotFound());
    }

}
