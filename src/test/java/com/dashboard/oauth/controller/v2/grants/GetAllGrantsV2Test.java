package com.dashboard.oauth.controller.v2.grants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v2/grant/")
class GetAllGrantsV2Test extends BaseV2GrantControllerTest {

    @Test
    @DisplayName("Should return 200 with grant list")
    void shouldReturn200WithGrantList() throws Exception {
        when(grantService.getGrants()).thenReturn(List.of(testGrantRead));

        mockMvc.perform(get("/api/v2/grant/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(testGrantName));
    }
}
