package com.dashboard.oauth.controller.v1.auth;

import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Story("Me flow")
@DisplayName("GET api/auth/me")
class MeTest extends BaseAuthControllerTest {

    @Test
    @DisplayName("Should return 200 with user info")
    void shouldReturn200WithUserInfo() throws Exception {
        User user = createTestUser();
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail(testEmail);

        when(authService.getCurrentUser(any())).thenReturn(userInfoRead);

        mockMvc.perform(get("/api/v1/auth/me")
                        .principal(() -> "user")
                        .with(request -> {
                            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()));
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail));
    }
}
