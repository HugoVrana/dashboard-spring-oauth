package com.dashboard.oauth.controller.v1.user;

import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.model.entities.user.User;
import io.qameta.allure.Description;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User Me Endpoints")
class UserMeTest extends BaseUserControllerTest {

    @Test
    @DisplayName("Should return current user info")
    @Description("Returns the authenticated user's information")
    void getMe_shouldReturnUserInfo() throws Exception {
        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());
        expectedResponse.setEmail(testEmail);
        expectedResponse.setEmailVerified(false);
        expectedResponse.setLocked(false);

        when(userService.getSelf(any(User.class))).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/v1/user/me")
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toHexString()))
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.locked").value(false));

        verify(userService).getSelf(any(User.class));
    }

    @Test
    @DisplayName("Should update user successfully")
    @Description("User can update their own data")
    void updateMe_shouldUpdateUser() throws Exception {
        String newEmail = faker.internet().emailAddress();
        UserSelfUpdate updateRequest = new UserSelfUpdate();
        updateRequest.setEmail(newEmail);
        updateRequest.setPassword("newPassword123");

        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());
        expectedResponse.setEmail(newEmail);

        when(userService.updateSelf(any(User.class), any(UserSelfUpdate.class))).thenReturn(expectedResponse);

        mockMvc.perform(put("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));

        verify(userService).updateSelf(any(User.class), any(UserSelfUpdate.class));
    }
}
