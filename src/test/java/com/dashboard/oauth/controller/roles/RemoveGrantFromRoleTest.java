package com.dashboard.oauth.controller.roles;

import com.dashboard.oauth.dataTransferObject.role.RoleGrantRequest;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.Role;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
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

        Grant grant = createTestGrant();
        Role role = createTestRole();
        role.setGrants(new ArrayList<>(List.of(grant)));

        Role updatedRole = createTestRole();
        updatedRole.setGrants(new ArrayList<>());

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.of(grant));
        when(roleService.updateRole(any(Role.class))).thenReturn(updatedRole);

        mockMvc.perform(delete("/api/role/grant")
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

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when grant not found")
    void removeGrant_shouldReturn404WhenGrantNotFound() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        Role role = createTestRole();
        role.setGrants(new ArrayList<>());

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 0 when grant not in role")
    void removeGrant_shouldReturn0WhenGrantNotInRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        Role role = createTestRole();
        role.setGrants(new ArrayList<>());

        Grant grant = createTestGrant();

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.of(grant));
        when(roleService.updateRole(any(Role.class))).thenReturn(role);

        mockMvc.perform(delete("/api/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("Should return 400 when role id or grant id is invalid")
    void removeGrant_shouldReturn400WhenRoleIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId("invalid-id");
        request.setGrantId(testGrantId.toHexString());

        mockMvc.perform(delete("/api/role/grant")
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

        Role role = createTestRole();
        role.setGrants(new ArrayList<>());

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));

        mockMvc.perform(delete("/api/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
