package com.dashboard.oauth.controller.v2.roles;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/v2/role/")
class CreateRoleV2Test extends BaseV2RoleControllerTest {

    @Test
    @DisplayName("Should return 200 with created role")
    void shouldReturn200WithCreatedRole() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        when(roleService.createRole(any(CreateRole.class))).thenReturn(testRoleRead);

        mockMvc.perform(post("/api/v2/role/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName));
    }

    @Test
    @DisplayName("Should return 409 when role already exists")
    void shouldReturn409_whenRoleAlreadyExists() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        when(roleService.createRole(any(CreateRole.class)))
                .thenThrow(new ConflictException("Role already exists"));

        mockMvc.perform(post("/api/v2/role/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isConflict());
    }
}
