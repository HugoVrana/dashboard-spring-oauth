package com.dashboard.oauth.controller.roles;

import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.model.entities.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST api/role/")
class AddRoleTest extends BaseRoleControllerTest {

    @Test
    @DisplayName("Should return 200 with created role")
    void shouldReturn200WhenSuccessful() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        Role createdRole = new Role();
        createdRole.set_id(testRoleId);
        createdRole.setName(testRoleName);

        RoleRead roleRead = new RoleRead();
        roleRead.setName(testRoleName);

        when(roleService.getRoleByName(testRoleName)).thenReturn(Optional.empty());
        when(roleService.createRole(any(Role.class))).thenReturn(createdRole);
        when(roleMapper.toRead(any(Role.class))).thenReturn(roleRead);

        mockMvc.perform(post("/api/v1/role/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName));
    }

    @Test
    @DisplayName("Should return 409 when role already exists")
    void shouldReturn409WhenRoleExists() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        Role existingRole = new Role();
        existingRole.setName(testRoleName);

        when(roleService.getRoleByName(testRoleName)).thenReturn(Optional.of(existingRole));

        mockMvc.perform(post("/api/v1/role/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isConflict());
    }
}
