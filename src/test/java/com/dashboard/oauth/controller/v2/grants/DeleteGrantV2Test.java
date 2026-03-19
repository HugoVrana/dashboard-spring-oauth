package com.dashboard.oauth.controller.v2.grants;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DELETE /api/v2/grant/{id}")
class DeleteGrantV2Test extends BaseV2GrantControllerTest {

    @Test
    @DisplayName("Should return 204 when deleted")
    void shouldReturn204_whenDeleted() throws Exception {
        doNothing().when(grantService).deleteGrant(any());

        mockMvc.perform(delete("/api/v2/grant/{id}", testGrantId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when grant not found")
    void shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Grant not found"))
                .when(grantService).deleteGrant(any());

        mockMvc.perform(delete("/api/v2/grant/{id}", testGrantId))
                .andExpect(status().isNotFound());
    }
}
