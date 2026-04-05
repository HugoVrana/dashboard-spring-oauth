package com.dashboard.oauth.controller.v1.roles;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.role.RoleGrantRequest;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Story("Remove grant from role flow")
@DisplayName("DELETE api/role/grant")
class RemoveGrantFromRoleTest extends BaseRoleControllerTest {

    @Test
    @DisplayName("Should return 200 with count affected")
    void removeGrant_shouldReturn200WithCountAffected() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        RoleRead roleRead = new RoleRead();
        roleRead.setName(testRoleName);

        when(roleService.removeGrantFromRole(testRoleId, testGrantId)).thenReturn(roleRead);

        mockMvc.perform(delete("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void removeGrant_shouldReturn404WhenRoleNotFound() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        when(roleService.removeGrantFromRole(testRoleId, testGrantId))
                .thenThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(delete("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when grant not assigned to role")
    void removeGrant_shouldReturn404WhenGrantNotAssigned() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        when(roleService.removeGrantFromRole(testRoleId, testGrantId))
                .thenThrow(new ResourceNotFoundException("Grant is not assigned to this role"));

        mockMvc.perform(delete("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when role id is invalid")
    void removeGrant_shouldReturn400WhenRoleIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId("invalid-id");
        request.setGrantId(testGrantId.toHexString());

        mockMvc.perform(delete("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when grant id is invalid")
    void removeGrant_shouldReturn400WhenGrantIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId("invalid-id");

        mockMvc.perform(delete("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
