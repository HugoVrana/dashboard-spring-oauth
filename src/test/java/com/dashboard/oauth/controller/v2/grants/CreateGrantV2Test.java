package com.dashboard.oauth.controller.v2.grants;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/v2/grant/")
class CreateGrantV2Test extends BaseV2GrantControllerTest {

    @Test
    @DisplayName("Should return 200 with created grant")
    void shouldReturn200WithCreatedGrant() throws Exception {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription(testGrantDescription);

        when(grantService.createGrant(any(GrantCreate.class))).thenReturn(testGrantRead);

        mockMvc.perform(post("/api/v2/grant/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testGrantName));
    }

    @Test
    @DisplayName("Should return 409 when grant already exists")
    void shouldReturn409_whenGrantAlreadyExists() throws Exception {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription(testGrantDescription);

        when(grantService.createGrant(any(GrantCreate.class)))
                .thenThrow(new ConflictException("Grant already exists"));

        mockMvc.perform(post("/api/v2/grant/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantCreate)))
                .andExpect(status().isConflict());
    }
}
