package com.dashboard.oauth.controller.v2.roles;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.role.RoleUpdate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PUT /api/v2/role/{id}")
class UpdateRoleV2Test extends BaseV2RoleControllerTest {

    @Test
    @DisplayName("Should return 200 with updated role")
    void shouldReturn200WithUpdatedRole() throws Exception {
        RoleUpdate update = new RoleUpdate();
        update.setName(testRoleName);

        when(roleService.updateRole(any(), any(RoleUpdate.class))).thenReturn(testRoleRead);

        mockMvc.perform(put("/api/v2/role/{id}", testRoleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName));
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void shouldReturn404_whenNotFound() throws Exception {
        RoleUpdate update = new RoleUpdate();
        update.setName(testRoleName);

        when(roleService.updateRole(any(), any(RoleUpdate.class)))
                .thenThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(put("/api/v2/role/{id}", testRoleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when name already exists")
    void shouldReturn409_whenNameConflict() throws Exception {
        RoleUpdate update = new RoleUpdate();
        update.setName(testRoleName);

        when(roleService.updateRole(any(), any(RoleUpdate.class)))
                .thenThrow(new ConflictException("Role with this name already exists"));

        mockMvc.perform(put("/api/v2/role/{id}", testRoleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when name is blank")
    void shouldReturn400_whenNameIsBlank() throws Exception {
        RoleUpdate update = new RoleUpdate();
        update.setName("");

        mockMvc.perform(put("/api/v2/role/{id}", testRoleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }
}
