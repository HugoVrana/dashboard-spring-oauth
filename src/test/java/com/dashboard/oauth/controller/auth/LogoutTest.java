package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.model.entities.User;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogoutTest extends BaseAuthControllerTest {

    @Test
    void shouldReturn200WhenSuccessful() throws Exception {
        User user = createTestUser();

        when(jwtService.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(userService.getUserByEmail(testEmail)).thenReturn(Optional.of(user));
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + testAccessToken))
                .andExpect(status().isOk());

        verify(authService).logout(user.get_id().toHexString());
    }

    @Test
    void shouldReturn400WhenUserNotFound() throws Exception {
        String unknownEmail = faker.internet().emailAddress();
        when(jwtService.extractUsername(testAccessToken)).thenReturn(unknownEmail);
        when(userService.getUserByEmail(unknownEmail)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + testAccessToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500WhenAuthorizationHeaderMissing() throws Exception {
        // Note: Returns 500 because MissingRequestHeaderException is caught by generic exception handler
        // Consider adding specific handler for MissingRequestHeaderException to return 400
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isInternalServerError());
    }
}
