package com.dashboard.oauth.controller.user;

import com.dashboard.oauth.dataTransferObject.user.UserSelfRead;
import com.dashboard.oauth.dataTransferObject.user.UserSelfUpdate;
import com.dashboard.oauth.model.entities.User;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

        when(userInfoMapper.toSelfRead(any(User.class))).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/user/me")
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toHexString()))
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.locked").value(false));

        verify(userInfoMapper).toSelfRead(any(User.class));
    }

    @Test
    @DisplayName("Should update user email successfully")
    @Description("User can update their own email address")
    void updateMe_shouldUpdateEmail() throws Exception {
        String newEmail = faker.internet().emailAddress();
        UserSelfUpdate updateRequest = new UserSelfUpdate();
        updateRequest.setEmail(newEmail);
        updateRequest.setPassword("newPassword123");

        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());
        expectedResponse.setEmail(newEmail);

        when(userService.getUserByEmail(newEmail)).thenReturn(Optional.empty());
        when(userService.saveUser(any(User.class))).thenReturn(testUser);
        when(userInfoMapper.toSelfRead(any(User.class))).thenReturn(expectedResponse);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        mockMvc.perform(put("/api/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail));

        verify(userService).saveUser(any(User.class));
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    @DisplayName("Should return 409 when email already in use")
    @Description("Cannot update to an email that belongs to another user")
    void updateMe_shouldReturn409WhenEmailInUse() throws Exception {
        String existingEmail = faker.internet().emailAddress();
        UserSelfUpdate updateRequest = new UserSelfUpdate();
        updateRequest.setEmail(existingEmail);
        updateRequest.setPassword("password123");

        User existingUser = createTestUser();
        existingUser.set_id(new org.bson.types.ObjectId());
        existingUser.setEmail(existingEmail);

        when(userService.getUserByEmail(existingEmail)).thenReturn(Optional.of(existingUser));

        mockMvc.perform(put("/api/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isConflict());

        verify(userService, never()).saveUser(any(User.class));
    }

    @Test
    @DisplayName("Should allow same email for same user")
    @Description("User can keep their existing email without conflict")
    void updateMe_shouldAllowSameEmailForSameUser() throws Exception {
        UserSelfUpdate updateRequest = new UserSelfUpdate();
        updateRequest.setEmail(testEmail);
        updateRequest.setPassword("newPassword123");

        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());
        expectedResponse.setEmail(testEmail);

        when(userService.getUserByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.saveUser(any(User.class))).thenReturn(testUser);
        when(userInfoMapper.toSelfRead(any(User.class))).thenReturn(expectedResponse);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

        mockMvc.perform(put("/api/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isOk());

        verify(userService).saveUser(any(User.class));
    }

    @Test
    @DisplayName("Should handle null email in update")
    @Description("When email is not provided, user data is still saved")
    void updateMe_shouldHandleNullEmail() throws Exception {
        UserSelfUpdate updateRequest = new UserSelfUpdate();

        UserSelfRead expectedResponse = new UserSelfRead();
        expectedResponse.setId(testUserId.toHexString());
        expectedResponse.setEmail(testEmail);

        when(userService.saveUser(any(User.class))).thenReturn(testUser);
        when(userInfoMapper.toSelfRead(any(User.class))).thenReturn(expectedResponse);

        mockMvc.perform(put("/api/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(request -> {
                            request.setUserPrincipal(testAuthentication);
                            return request;
                        }))
                .andExpect(status().isOk());

        verify(userService).saveUser(any(User.class));
        verify(userService, never()).getUserByEmail(any());
    }
}
