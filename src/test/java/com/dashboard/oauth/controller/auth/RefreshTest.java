package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.RefreshTokenRequest;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Story("Refresh flow")
@DisplayName("POST api/auth/refresh")
class RefreshTest extends BaseAuthControllerTest {

    @Test
    @DisplayName("Should return 200 with new tokens")
    void shouldReturn200WithNewTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(testRefreshToken);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(testAccessToken);
        authResponse.setRefreshToken(faker.regexify("[a-zA-Z0-9]{32}"));
        authResponse.setExpiresIn(86400000L);

        when(authService.refreshToken(testRefreshToken)).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(testAccessToken));
    }

    @Test
    @DisplayName("Should return 500 when refresh token is invalid")
    void shouldReturn500WhenRefreshTokenInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        when(authService.refreshToken("invalid-refresh-token"))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return 500 when refresh token expired")
    void shouldReturn500WhenRefreshTokenExpired() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(testRefreshToken);

        when(authService.refreshToken(testRefreshToken))
                .thenThrow(new RuntimeException("Refresh token expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}