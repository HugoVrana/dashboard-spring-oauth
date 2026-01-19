package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginTest extends BaseAuthControllerTest {

    @Test
    void shouldReturn200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(testAccessToken);
        authResponse.setRefreshToken(testRefreshToken);
        authResponse.setExpiresIn(86400000L);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(testAccessToken))
                .andExpect(jsonPath("$.refreshToken").value(testRefreshToken));
    }
}
