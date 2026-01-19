package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeTest extends BaseAuthControllerTest {

    @Test
    void shouldReturn200WithUserInfo() throws Exception {
        User user = createTestUser();
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail(testEmail);

        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        mockMvc.perform(get("/api/auth/me")
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
