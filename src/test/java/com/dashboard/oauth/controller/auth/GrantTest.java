package com.dashboard.oauth.controller.auth;

import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.model.entities.Grant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GrantTest extends BaseAuthControllerTest {

    @Test
    void shouldReturn200WhenSuccessful() throws Exception {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription(testGrantDescription);

        Grant createdGrant = createTestGrant();

        GrantRead grantRead = new GrantRead();
        grantRead.setName(testGrantName);
        grantRead.setDescription(testGrantDescription);

        when(grantService.getGrantByName(testGrantName)).thenReturn(Optional.empty());
        when(grantService.createGrant(any(Grant.class))).thenReturn(createdGrant);
        when(grantMapper.toRead(any(Grant.class))).thenReturn(grantRead);

        mockMvc.perform(post("/api/auth/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testGrantName))
                .andExpect(jsonPath("$.description").value(testGrantDescription));
    }

    @Test
    void shouldReturn409WhenGrantExists() throws Exception {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription(testGrantDescription);

        Grant existingGrant = createTestGrant();

        when(grantService.getGrantByName(testGrantName)).thenReturn(Optional.of(existingGrant));

        mockMvc.perform(post("/api/auth/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantCreate)))
                .andExpect(status().isConflict());
    }
}
