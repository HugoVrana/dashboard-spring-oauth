package com.dashboard.oauth.controller.v2.grants;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.controller.v2.GrantController;
import com.dashboard.oauth.dataTransferObject.grant.GrantRead;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.service.GrantService;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Epic("Grants V2")
@Feature("Grants V2 API")
@Tag("v2-controller-grants")
@WebMvcTest(GrantController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
@WithMockUser(authorities = {"dashboard-oauth-grant-create", "dashboard-oauth-grant-delete"})
public abstract class BaseV2GrantControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected GrantService grantService;

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected UserDetailsService springUserDetailsService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

    protected final Faker faker = new Faker();

    protected String testGrantId;
    protected String testGrantName;
    protected String testGrantDescription;
    protected GrantRead testGrantRead;

    @BeforeEach
    void setUpBase() {
        testGrantId = new ObjectId().toHexString();
        testGrantName = faker.expression("#{letterify 'GRANT_????'}").toUpperCase();
        testGrantDescription = faker.lorem().sentence();

        testGrantRead = new GrantRead();
        testGrantRead.setId(testGrantId);
        testGrantRead.setName(testGrantName);
        testGrantRead.setDescription(testGrantDescription);
    }
}
