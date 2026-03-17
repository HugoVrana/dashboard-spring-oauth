package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Story("Login flow")
@DisplayName("POST api/auth/login")
class LoginTest extends BaseAuthControllerTest {

    @Test
    @DisplayName("Should return 200 with tokens on successful login")
    void shouldReturn200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        UserInfoRead userInfo = new UserInfoRead();
        userInfo.setId(testUserId.toHexString());
        userInfo.setEmail(testEmail);

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(testAccessToken);
        authResponse.setRefreshToken(testRefreshToken);
        authResponse.setExpiresIn(86400000L);
        authResponse.setUser(userInfo);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(testAccessToken))
                .andExpect(jsonPath("$.refreshToken").value(testRefreshToken));
    }
}
