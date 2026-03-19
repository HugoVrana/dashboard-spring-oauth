package com.dashboard.oauth.controller.v2.roles;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v2/role/{id}")
class GetRoleByIdV2Test extends BaseV2RoleControllerTest {

    @Test
    @DisplayName("Should return 200 with role when exists")
    void shouldReturn200WithRole_whenExists() throws Exception {
        when(roleService.getRoleReadById(any())).thenReturn(testRoleRead);

        mockMvc.perform(get("/api/v2/role/{id}", testRoleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName));
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void shouldReturn404_whenNotFound() throws Exception {
        when(roleService.getRoleReadById(any()))
                .thenThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(get("/api/v2/role/{id}", testRoleId))
                .andExpect(status().isNotFound());
    }
}
