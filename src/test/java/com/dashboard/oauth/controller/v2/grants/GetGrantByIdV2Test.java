package com.dashboard.oauth.controller.v2.grants;

import com.dashboard.common.model.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/v2/grant/{id}")
class GetGrantByIdV2Test extends BaseV2GrantControllerTest {

    @Test
    @DisplayName("Should return 200 with grant when exists")
    void shouldReturn200WithGrant_whenExists() throws Exception {
        when(grantService.getGrantReadById(any())).thenReturn(testGrantRead);

        mockMvc.perform(get("/api/v2/grant/{id}", testGrantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testGrantName));
    }

    @Test
    @DisplayName("Should return 404 when grant not found")
    void shouldReturn404_whenNotFound() throws Exception {
        when(grantService.getGrantReadById(any()))
                .thenThrow(new ResourceNotFoundException("Grant not found"));

        mockMvc.perform(get("/api/v2/grant/{id}", testGrantId))
                .andExpect(status().isNotFound());
    }
}
