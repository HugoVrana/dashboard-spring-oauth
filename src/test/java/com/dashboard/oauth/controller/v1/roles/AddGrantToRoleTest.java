package com.dashboard.oauth.controller.v1.roles;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.role.RoleGrantRequest;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST api/role/grant")
class AddGrantToRoleTest extends BaseRoleControllerTest {

    @Test
    @DisplayName("Should return 200 when successful")
    void addGrant_shouldReturn200WhenSuccessful() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        RoleRead roleRead = new RoleRead();
        roleRead.setName(testRoleName);

        when(roleService.addGrantToRole(testRoleId, testGrantId)).thenReturn(roleRead);

        mockMvc.perform(post("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 when role id is invalid")
    void addGrant_shouldReturn400WhenRoleIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId("invalid-id");
        request.setGrantId(testGrantId.toHexString());

        mockMvc.perform(post("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when grant id is invalid")
    void addGrant_shouldReturn400WhenGrantIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId("invalid-id");

        mockMvc.perform(post("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void addGrant_shouldReturn404WhenRoleNotFound() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        when(roleService.addGrantToRole(testRoleId, testGrantId))
                .thenThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(post("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when grant not found")
    void addGrant_shouldReturn404WhenGrantNotFound() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        when(roleService.addGrantToRole(testRoleId, testGrantId))
                .thenThrow(new ResourceNotFoundException("Grant not found"));

        mockMvc.perform(post("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when role already has grant")
    void addGrant_shouldReturn409WhenRoleAlreadyHasGrant() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        when(roleService.addGrantToRole(testRoleId, testGrantId))
                .thenThrow(new ConflictException("Grant is already assigned to this role"));

        mockMvc.perform(post("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
