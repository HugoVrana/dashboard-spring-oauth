package com.dashboard.oauth.controller.grants;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.Audit;
import com.dashboard.oauth.controller.GrantController;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;

@Epic("Grants")
@Feature("Grants")
@Tag("controller-grants")
@WebMvcTest(GrantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
public abstract class BaseGrantControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IGrantService grantService;

    @MockitoBean
    protected IGrantMapper grantMapper;

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected UserDetailsService springUserDetailsService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

    protected final Faker faker = new Faker();

    protected ObjectId testGrantId;
    protected String testGrantName;
    protected String testGrantDescription;

    @BeforeEach
    void setUpBase() {
        testGrantId = new ObjectId();
        testGrantName = faker.expression("#{letterify 'GRANT_????'}").toUpperCase();
        testGrantDescription = faker.lorem().sentence();
    }

    protected Grant createTestGrant() {
        Grant grant = new Grant();
        grant.set_id(testGrantId);
        grant.setName(testGrantName);
        grant.setDescription(testGrantDescription);

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        grant.setAudit(audit);

        return grant;
    }
}
