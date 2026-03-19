package com.dashboard.oauth.controller.v2.users;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DELETE /api/v2/user/{id}")
class DeleteUserV2Test extends BaseV2UserControllerTest {

    @Test
    @DisplayName("Should return 204 when deleted")
    void shouldReturn204_whenDeleted() throws Exception {
        doNothing().when(userService).deleteUser(any());

        mockMvc.perform(delete("/api/v2/user/{id}", testUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).deleteUser(any());

        mockMvc.perform(delete("/api/v2/user/{id}", testUserId))
                .andExpect(status().isNotFound());
    }
}
