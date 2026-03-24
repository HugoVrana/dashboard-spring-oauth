package com.dashboard.oauth.controller.v2.oauthclients;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DELETE /v2/oauthclients/{id}")
class DeleteClientV2Test extends BaseV2OAuthClientControllerTest {

    @Test
    @DisplayName("Should return 204 when deleted")
    void shouldReturn204_whenDeleted() throws Exception {
        doNothing().when(oAuthClientService).deleteClient(any());

        mockMvc.perform(delete("/v2/oauthclients/{id}", testClientId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when client not found")
    void shouldReturn404_whenClientNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("OAuth client not found"))
                .when(oAuthClientService).deleteClient(any());

        mockMvc.perform(delete("/v2/oauthclients/{id}", testClientId))
                .andExpect(status().isNotFound());
    }

}
