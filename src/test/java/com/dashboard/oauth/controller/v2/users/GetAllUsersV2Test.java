package com.dashboard.oauth.controller.v2.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v2/user/")
class GetAllUsersV2Test extends BaseV2UserControllerTest {

    @Test
    @DisplayName("Should return 200 with user list")
    void shouldReturn200WithUserList() throws Exception {
        when(userService.getUsers()).thenReturn(List.of(testUserAdminRead));

        mockMvc.perform(get("/api/v2/user/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(testEmail));
    }
}
