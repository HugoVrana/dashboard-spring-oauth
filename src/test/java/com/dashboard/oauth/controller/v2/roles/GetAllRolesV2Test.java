package com.dashboard.oauth.controller.v2.roles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v2/role/")
class GetAllRolesV2Test extends BaseV2RoleControllerTest {

    @Test
    @DisplayName("Should return 200 with role list")
    void shouldReturn200WithRoleList() throws Exception {
        when(roleService.getRoles()).thenReturn(List.of(testRoleRead));

        mockMvc.perform(get("/api/v2/role/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(testRoleName));
    }
}
