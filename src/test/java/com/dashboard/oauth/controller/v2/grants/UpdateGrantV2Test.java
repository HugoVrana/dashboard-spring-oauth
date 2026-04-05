package com.dashboard.oauth.controller.v2.grants;

import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.grant.GrantUpdate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PUT /api/v2/grant/{id}")
class UpdateGrantV2Test extends BaseV2GrantControllerTest {

    @Test
    @DisplayName("Should return 200 with updated grant")
    void shouldReturn200WithUpdatedGrant() throws Exception {
        GrantUpdate update = new GrantUpdate();
        update.setName(testGrantName);
        update.setDescription(testGrantDescription);

        when(grantService.updateGrant(any(), any(GrantUpdate.class))).thenReturn(testGrantRead);

        mockMvc.perform(put("/api/v2/grant/{id}", testGrantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testGrantName))
                .andExpect(jsonPath("$.description").value(testGrantDescription));
    }

    @Test
    @DisplayName("Should return 404 when grant not found")
    void shouldReturn404_whenNotFound() throws Exception {
        GrantUpdate update = new GrantUpdate();
        update.setName(testGrantName);

        when(grantService.updateGrant(any(), any(GrantUpdate.class)))
                .thenThrow(new ResourceNotFoundException("Grant not found"));

        mockMvc.perform(put("/api/v2/grant/{id}", testGrantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 409 when name already exists")
    void shouldReturn409_whenNameConflict() throws Exception {
        GrantUpdate update = new GrantUpdate();
        update.setName(testGrantName);

        when(grantService.updateGrant(any(), any(GrantUpdate.class)))
                .thenThrow(new ConflictException("Grant with this name already exists"));

        mockMvc.perform(put("/api/v2/grant/{id}", testGrantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should return 400 when name is blank")
    void shouldReturn400_whenNameIsBlank() throws Exception {
        GrantUpdate update = new GrantUpdate();
        update.setName("");

        mockMvc.perform(put("/api/v2/grant/{id}", testGrantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest());
    }
}
