package com.dashboard.oauth.controller.auth;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.role.AddRoleRequest;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Story("Add user role flow")
@DisplayName("POST api/auth/user/role")
class UserRoleTest extends BaseAuthControllerTest {

    @Test
    @DisplayName("Should return 200 when successful")
    void shouldReturn200WhenSuccessful() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId(testRoleId.toHexString());

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken(testAccessToken);

        when(authService.addUserRole(any(AddRoleRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 when user id is invalid")
    void shouldReturn400WhenUserIdInvalid() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId("invalid-id");
        request.setRoleId(testRoleId.toHexString());

        when(authService.addUserRole(any(AddRoleRequest.class)))
                .thenThrow(new InvalidRequestException("User id is invalid."));

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when role id is invalid")
    void shouldReturn400WhenRoleIdInvalid() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId("invalid-id");

        when(authService.addUserRole(any(AddRoleRequest.class)))
                .thenThrow(new InvalidRequestException("Role id is invalid."));

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId(testRoleId.toHexString());

        when(authService.addUserRole(any(AddRoleRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void shouldReturn404WhenRoleNotFound() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId(testRoleId.toHexString());

        when(authService.addUserRole(any(AddRoleRequest.class)))
                .thenThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when user already has role")
    void shouldReturn409WhenUserAlreadyHasRole() throws Exception {
        AddRoleRequest request = new AddRoleRequest();
        request.setUserId(testUserId.toHexString());
        request.setRoleId(testRoleId.toHexString());

        when(authService.addUserRole(any(AddRoleRequest.class)))
                .thenThrow(new ConflictException("User already has role"));

        mockMvc.perform(post("/api/auth/user/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
