package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.RoleGrantRequest;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleGrantTest extends BaseAuthControllerTest {

    // ==================== Add Grant to Role Tests ====================

    @Test
    void addGrant_shouldReturn200WhenSuccessful() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        Role role = createTestRole();
        role.setGrants(new ArrayList<>());

        Grant grant = createTestGrant();

        Role updatedRole = createTestRole();
        updatedRole.setGrants(List.of(grant));

        RoleRead roleRead = new RoleRead();
        roleRead.setName(testRoleName);

        GrantRead grantRead = new GrantRead();
        grantRead.setName(testGrantName);

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.of(grant));
        when(roleService.updateRole(any(Role.class))).thenReturn(updatedRole);
        when(roleMapper.toRead(any(Role.class))).thenReturn(roleRead);
        when(grantMapper.toRead(any(Grant.class))).thenReturn(grantRead);

        mockMvc.perform(post("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void addGrant_shouldReturn400WhenRoleIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId("invalid-id");
        request.setGrantId(testGrantId.toHexString());

        mockMvc.perform(post("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addGrant_shouldReturn400WhenGrantIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId("invalid-id");

        Role role = createTestRole();

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));

        mockMvc.perform(post("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addGrant_shouldReturn404WhenRoleNotFound() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addGrant_shouldReturn404WhenGrantNotFound() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        Role role = createTestRole();

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addGrant_shouldReturn409WhenRoleAlreadyHasGrant() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        Grant grant = createTestGrant();
        Role role = createTestRole();
        role.setGrants(new ArrayList<>(List.of(grant)));

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.of(grant));

        mockMvc.perform(post("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ==================== Remove Grant from Role Tests ====================

    @Test
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

        mockMvc.perform(delete("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    void removeGrant_shouldReturn404WhenRoleNotFound() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeGrant_shouldReturn404WhenGrantNotFound() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId(testGrantId.toHexString());

        Role role = createTestRole();
        role.setGrants(new ArrayList<>());

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
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

        mockMvc.perform(delete("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void removeGrant_shouldReturn400WhenRoleIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId("invalid-id");
        request.setGrantId(testGrantId.toHexString());

        mockMvc.perform(delete("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeGrant_shouldReturn400WhenGrantIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId("invalid-id");

        Role role = createTestRole();
        role.setGrants(new ArrayList<>());

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));

        mockMvc.perform(delete("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
