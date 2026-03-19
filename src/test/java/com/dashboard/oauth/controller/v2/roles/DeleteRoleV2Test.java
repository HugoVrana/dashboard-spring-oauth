package com.dashboard.oauth.controller.v2.roles;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("DELETE /api/v2/role/{id}")
class DeleteRoleV2Test extends BaseV2RoleControllerTest {

    @Test
    @DisplayName("Should return 204 when deleted")
    void shouldReturn204_whenDeleted() throws Exception {
        doNothing().when(roleService).deleteRole(any());

        mockMvc.perform(delete("/api/v2/role/{id}", testRoleId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void shouldReturn404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Role not found"))
                .when(roleService).deleteRole(any());

        mockMvc.perform(delete("/api/v2/role/{id}", testRoleId))
                .andExpect(status().isNotFound());
    }
}
