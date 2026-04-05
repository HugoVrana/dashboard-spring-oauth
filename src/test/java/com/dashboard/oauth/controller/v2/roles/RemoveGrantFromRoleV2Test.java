package com.dashboard.oauth.controller.v2.roles;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DELETE /api/v2/role/{id}/grants/{grantId}")
class RemoveGrantFromRoleV2Test extends BaseV2RoleControllerTest {

    private String testGrantId;

    @BeforeEach
    void setUp() {
        testGrantId = new ObjectId().toHexString();
        testRoleRead.setGrants(List.of());
    }

    @Test
    @DisplayName("Should return 200 with updated role")
    void shouldReturn200WithUpdatedRole() throws Exception {
        when(roleService.removeGrantFromRole(any(), any())).thenReturn(testRoleRead);

        mockMvc.perform(delete("/api/v2/role/{id}/grants/{grantId}", testRoleId, testGrantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName))
                .andExpect(jsonPath("$.grants").isEmpty());
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void shouldReturn404_whenRoleNotFound() throws Exception {
        when(roleService.removeGrantFromRole(any(), any()))
                .thenThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(delete("/api/v2/role/{id}/grants/{grantId}", testRoleId, testGrantId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when grant not assigned to role")
    void shouldReturn404_whenGrantNotAssigned() throws Exception {
        when(roleService.removeGrantFromRole(any(), any()))
                .thenThrow(new ResourceNotFoundException("Grant is not assigned to this role"));

        mockMvc.perform(delete("/api/v2/role/{id}/grants/{grantId}", testRoleId, testGrantId))
                .andExpect(status().isNotFound());
    }
}
