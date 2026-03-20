package com.dashboard.oauth.controller.v2.roles;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.controller.v2.RoleController;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.service.interfaces.IRoleService;
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

@Epic("Roles V2")
@Feature("Roles V2 API")
@Tag("v2-controller-roles")
@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
@WithMockUser(authorities = {"dashboard-oauth-role-create", "dashboard-oauth-role-delete"})
public abstract class BaseV2RoleControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IRoleService roleService;

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected UserDetailsService springUserDetailsService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

    protected final Faker faker = new Faker();

    protected String testRoleId;
    protected String testRoleName;
    protected RoleRead testRoleRead;

    @BeforeEach
    void setUpBase() {
        testRoleId = new ObjectId().toHexString();
        testRoleName = faker.expression("#{letterify 'ROLE_????'}").toUpperCase();

        testRoleRead = new RoleRead();
        testRoleRead.setId(testRoleId);
        testRoleRead.setName(testRoleName);
    }
}
