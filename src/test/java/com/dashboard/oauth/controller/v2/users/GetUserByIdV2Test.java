package com.dashboard.oauth.controller.v2.users;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v2/user/{id}")
class GetUserByIdV2Test extends BaseV2UserControllerTest {

    @Test
    @DisplayName("Should return 200 with user when exists")
    void shouldReturn200WithUser_whenExists() throws Exception {
        when(userService.getUserAdminReadById(any())).thenReturn(testUserAdminRead);

        mockMvc.perform(get("/api/v2/user/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404_whenNotFound() throws Exception {
        when(userService.getUserAdminReadById(any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/v2/user/{id}", testUserId))
                .andExpect(status().isNotFound());
    }
}
