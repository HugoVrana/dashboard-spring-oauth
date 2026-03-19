package com.dashboard.oauth.controller.v2.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v2/user/search")
class SearchUsersV2Test extends BaseV2UserControllerTest {

    @Test
    @DisplayName("Should return 200 with matching users")
    void shouldReturn200WithMatchingUsers() throws Exception {
        when(userService.searchUsers("test")).thenReturn(List.of(testUserAdminRead));

        mockMvc.perform(get("/api/v2/user/search").param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(testEmail));
    }
}
