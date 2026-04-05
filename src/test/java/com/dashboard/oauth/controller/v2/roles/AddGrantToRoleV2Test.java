package com.dashboard.oauth.controller.v2.roles;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/v2/role/{id}/grants/{grantId}")
class AddGrantToRoleV2Test extends BaseV2RoleControllerTest {

    private String testGrantId;

    @BeforeEach
    void setUp() {
        testGrantId = new ObjectId().toHexString();

        GrantRead grantRead = new GrantRead();
        grantRead.setId(testGrantId);
        grantRead.setName("some-grant");
        testRoleRead.setGrants(List.of(grantRead));
    }

    @Test
    @DisplayName("Should return 200 with updated role")
    void shouldReturn200WithUpdatedRole() throws Exception {
        when(roleService.addGrantToRole(any(), any())).thenReturn(testRoleRead);

        mockMvc.perform(post("/api/v2/role/{id}/grants/{grantId}", testRoleId, testGrantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName))
                .andExpect(jsonPath("$.grants[0].id").value(testGrantId));
    }

    @Test
    @DisplayName("Should return 404 when role not found")
    void shouldReturn404_whenRoleNotFound() throws Exception {
        when(roleService.addGrantToRole(any(), any()))
                .thenThrow(new ResourceNotFoundException("Role not found"));

        mockMvc.perform(post("/api/v2/role/{id}/grants/{grantId}", testRoleId, testGrantId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when grant not found")
    void shouldReturn404_whenGrantNotFound() throws Exception {
        when(roleService.addGrantToRole(any(), any()))
                .thenThrow(new ResourceNotFoundException("Grant not found"));

        mockMvc.perform(post("/api/v2/role/{id}/grants/{grantId}", testRoleId, testGrantId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when grant already assigned")
    void shouldReturn409_whenGrantAlreadyAssigned() throws Exception {
        when(roleService.addGrantToRole(any(), any()))
                .thenThrow(new ConflictException("Grant is already assigned to this role"));

        mockMvc.perform(post("/api/v2/role/{id}/grants/{grantId}", testRoleId, testGrantId))
                .andExpect(status().isConflict());
    }
}
