package com.dashboard.oauth.controller.v2.users;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.user.UserAdminUpdate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PUT /api/v2/user/{id}")
class UpdateUserV2Test extends BaseV2UserControllerTest {

    @Test
    @DisplayName("Should return 200 with updated user")
    void shouldReturn200WithUpdatedUser() throws Exception {
        UserAdminUpdate update = new UserAdminUpdate();
        update.setEmail(testEmail);

        when(userService.updateUser(any(), any(UserAdminUpdate.class))).thenReturn(testUserAdminRead);

        mockMvc.perform(put("/api/v2/user/{id}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404_whenNotFound() throws Exception {
        UserAdminUpdate update = new UserAdminUpdate();
        update.setEmail(testEmail);

        when(userService.updateUser(any(), any(UserAdminUpdate.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(put("/api/v2/user/{id}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }
}
