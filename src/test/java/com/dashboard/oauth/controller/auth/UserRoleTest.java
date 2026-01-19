package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.dataTransferObject.role.AddRoleRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.model.UserInfo;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserRoleTest extends BaseAuthControllerTest {

    @Test
    void shouldReturn200WhenSuccessful() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId(testRoleId.toHexString());

        User user = createTestUser();
        user.setRoles(new ArrayList<>());

        Role role = createTestRole();

        User updatedUser = createTestUser();
        updatedUser.setRoles(new ArrayList<>(List.of(role)));

        UserInfoRead userInfoRead = new UserInfoRead();
        userInfoRead.setEmail(testEmail);

        when(userService.getUserById(testUserId)).thenReturn(Optional.of(user));
        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(userService.saveUser(any(User.class))).thenReturn(updatedUser);
        when(userInfoMapper.toRead(any(UserInfo.class))).thenReturn(userInfoRead);

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenUserIdInvalid() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId("invalid-id");
        request.setRoleId(testRoleId.toHexString());

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenRoleIdInvalid() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId("invalid-id");

        User user = createTestUser();

        when(userService.getUserById(testUserId)).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId(testRoleId.toHexString());

        when(userService.getUserById(testUserId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenRoleNotFound() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId(testRoleId.toHexString());

        User user = createTestUser();

        when(userService.getUserById(testUserId)).thenReturn(Optional.of(user));
        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenUserAlreadyHasRole() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId(testRoleId.toHexString());

        Role role = createTestRole();
        User user = createTestUser();
        user.setRoles(new ArrayList<>(List.of(role)));

        when(userService.getUserById(testUserId)).thenReturn(Optional.of(user));
        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
