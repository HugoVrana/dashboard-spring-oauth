package com.dashboard.oauth.controller.v2.users;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/v2/user/{id}/reset-password")
class TriggerPasswordResetV2Test extends BaseV2UserControllerTest {

    @Test
    @DisplayName("Should return 204 when password reset triggered successfully")
    void shouldReturn204_whenTriggered() throws Exception {
        doNothing().when(userService).triggerPasswordReset(any());

        mockMvc.perform(post("/api/v2/user/{id}/reset-password", testUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).triggerPasswordReset(any());

        mockMvc.perform(post("/api/v2/user/{id}/reset-password", testUserId))
                .andExpect(status().isNotFound());
    }
}
