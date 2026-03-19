package com.dashboard.oauth.controller.v2.users;

import com.dashboard.common.model.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/v2/user/{id}/unblock")
class UnblockUserV2Test extends BaseV2UserControllerTest {

    @Test
    @DisplayName("Should return 204 when user unblocked successfully")
    void shouldReturn204_whenUnblocked() throws Exception {
        doNothing().when(userService).unblockUser(any());

        mockMvc.perform(post("/api/v2/user/{id}/unblock", testUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 409 when user is not blocked")
    void shouldReturn409_whenNotBlocked() throws Exception {
        doThrow(new ConflictException("User is not blocked"))
                .when(userService).unblockUser(any());

        mockMvc.perform(post("/api/v2/user/{id}/unblock", testUserId))
                .andExpect(status().isConflict());
    }
}
