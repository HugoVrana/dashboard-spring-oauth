package com.dashboard.oauth.controller.v1.roles;

import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.dataTransferObject.role.RoleGrantRequest;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
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

        mockMvc.perform(post("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return 400 when role id or grant id is invalid")
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
    @DisplayName("Should return 400 when role id or grant id is invalid")
    void addGrant_shouldReturn400WhenGrantIdInvalid() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId.toHexString());
        request.setGrantId("invalid-id");

        Role role = createTestRole();

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));

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

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.empty());

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

        Role role = createTestRole();

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.empty());

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

        Grant grant = createTestGrant();
        Role role = createTestRole();
        role.setGrants(new ArrayList<>(List.of(grant)));

        when(roleService.getRoleById(testRoleId)).thenReturn(Optional.of(role));
        when(grantService.getGrantById(testGrantId)).thenReturn(Optional.of(grant));

        mockMvc.perform(post("/api/v1/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}