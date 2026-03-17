package com.dashboard.oauth.controller.activity;

import com.dashboard.common.model.ActivityEvent;
import io.qameta.allure.Description;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Story("Get Recent Activity Events")
class GetRecentEventsTest extends BaseActivityControllerTest {

    @Test
    @DisplayName("Should return recent events with default limit")
    @Description("Verifies that the endpoint returns recent events using default limit of 50")
    void shouldReturnRecentEventsWithDefaultLimit() throws Exception {
        List<ActivityEvent> events = Arrays.asList(
                createTestEvent("INVOICE_CREATED"),
                createTestEvent("INVOICE_UPDATED")
        );
        when(activityFeedService.getRecentEvents(50)).thenReturn(events);

        mockMvc.perform(get("/api/v1/activity/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(activityFeedService).getRecentEvents(50);
    }

    @Test
    @DisplayName("Should return recent events with custom limit")
    @Description("Verifies that the endpoint respects the custom limit parameter")
    void shouldReturnRecentEventsWithCustomLimit() throws Exception {
        List<ActivityEvent> events = Collections.singletonList(createTestEvent("INVOICE_DELETED"));
        when(activityFeedService.getRecentEvents(10)).thenReturn(events);

        mockMvc.perform(get("/api/v1/activity/recent")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(activityFeedService).getRecentEvents(10);
    }

    @Test
    @DisplayName("Should return empty list when no events")
    @Description("Verifies that the endpoint returns empty list when no events exist")
    void shouldReturnEmptyListWhenNoEvents() throws Exception {
        when(activityFeedService.getRecentEvents(anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/activity/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should return event with all fields")
    @Description("Verifies that the response includes all event fields")
    void shouldReturnEventWithAllFields() throws Exception {
        when(activityFeedService.getRecentEvents(50)).thenReturn(Collections.singletonList(testEvent));

        mockMvc.perform(get("/api/v1/activity/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testEvent.getId()))
                .andExpect(jsonPath("$[0].type").value(testEvent.getType()))
                .andExpect(jsonPath("$[0].actorId").value(testEvent.getActorId()))
                .andExpect(jsonPath("$[0].metadata").exists());
    }
}
