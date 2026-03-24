package com.dashboard.oauth.controller.v2.oauthclients;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /v2/oauthclients/{id}")
class GetClientV2Test extends BaseV2OAuthClientControllerTest {

    @Test
    @DisplayName("Should return 200 with client when found")
    void shouldReturn200_whenClientFound() throws Exception {
        when(oAuthClientService.getClient(any())).thenReturn(testClientRead);

        mockMvc.perform(get("/v2/oauthclients/{id}", testClientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testClientId))
                .andExpect(jsonPath("$.clientSecret").doesNotExist());
    }

    @Test
    @DisplayName("Should return 404 when client not found")
    void shouldReturn404_whenClientNotFound() throws Exception {
        when(oAuthClientService.getClient(any()))
                .thenThrow(new ResourceNotFoundException("OAuth client not found"));

        mockMvc.perform(get("/v2/oauthclients/{id}", testClientId))
                .andExpect(status().isNotFound());
    }
}
