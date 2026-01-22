package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Story("Register flow")
@DisplayName("POST api/auth/register")
class RegisterTest extends BaseAuthControllerTest {

    @Test
    @DisplayName("Should return 201 with user info")
    void shouldReturn201WhenSuccessful() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRoleId(testRoleId.toHexString());

        Role role = createTestRole();
        User user = createTestUser();
        UserInfo userInfo = new UserInfo();
        userInfo.setId(testUserId);
        userInfo.setEmail(testEmail);

        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail(testEmail);

        when(userDetailsService.loadUserByUsername(testEmail))
                .thenThrow(new UsernameNotFoundException("User not found"));
        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(authService.register(any(RegisterRequest.class))).thenReturn(user);
        when(userService.saveUser(any(User.class))).thenReturn(user);
        when(userInfoMapper.toUserInfo(any(User.class))).thenReturn(userInfo);
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);
        when(roleMapper.toRead(any(Role.class))).thenReturn(new RoleRead());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    @DisplayName("Should return 409 when user exists")
    void shouldReturn409WhenUserExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRoleId(testRoleId.toHexString());

        UserDetailsImpl existingUserDetails = mock(UserDetailsImpl.class);
        when(userDetailsService.loadUserByUsername(testEmail))
                .thenReturn(existingUserDetails);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void shouldReturn400WhenRoleIdInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRoleId("invalid-role-id");

        when(userDetailsService.loadUserByUsername(testEmail))
                .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void shouldReturn404WhenRoleNotFound() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRoleId(testRoleId.toHexString());

        when(userDetailsService.loadUserByUsername(testEmail))
                .thenThrow(new UsernameNotFoundException("User not found"));
        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 500 when user service throws exception")
    void shouldReturn400WhenEmailMissing() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setPassword(testPassword);
        request.setRoleId(testRoleId.toHexString());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 500 when user service throws exception")
    void shouldReturn400WhenPasswordMissing() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setRoleId(testRoleId.toHexString());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 500 when user service throws exception")
    void shouldReturn400WhenRoleIdMissing() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void shouldReturn400WhenEmailInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-an-email");
        request.setPassword(testPassword);
        request.setRoleId(testRoleId.toHexString());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
