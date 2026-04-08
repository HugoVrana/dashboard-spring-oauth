package com.dashboard.oauth.controller.v1.auth;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Story("Logout flow")
@DisplayName("POST api/auth/logout")
class LogoutTest extends BaseAuthControllerTest {

    @Test
    @DisplayName("Should return 200 when successful")
    void shouldReturn200WhenSuccessful() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + testAccessToken))
                .andExpect(status().isOk());

        verify(authService).logout("Bearer " + testAccessToken);
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(authService).logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + testAccessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 500 when authorization header missing")
    void shouldReturn500WhenAuthorizationHeaderMissing() throws Exception {
        // Note: Returns 500 because MissingRequestHeaderException is caught by generic exception handler
        // Consider adding specific handler for MissingRequestHeaderException to return 400
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isInternalServerError());
    }
}
