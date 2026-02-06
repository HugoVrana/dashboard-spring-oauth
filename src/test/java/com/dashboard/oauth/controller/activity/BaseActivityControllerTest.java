package com.dashboard.oauth.controller.activity;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.ActivityEvent;
import com.dashboard.oauth.controller.config.TestConfig;
import com.dashboard.oauth.controller.ActivityController;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.service.JwtService;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Epic("Activity Feed")
@Feature("Activity API")
@Tag("controller-activity")
@WebMvcTest(ActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
@WithMockUser(username = "testUser")
public abstract class BaseActivityControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IActivityFeedService activityFeedService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected JwtService jwtService;

    protected ActivityEvent testEvent;

    @BeforeEach
    void setUpBase() {
        testEvent = createTestEvent("INVOICE_CREATED");
    }

    protected ActivityEvent createTestEvent(String type) {
        ActivityEvent event = ActivityEvent
                .builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .type(type)
                .actorId("testUser")
                .metadata(Map.of("invoiceId", "test123", "amount", 100.0))
                .build();
        return event;
    }
}
