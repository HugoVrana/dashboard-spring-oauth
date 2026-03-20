package com.dashboard.oauth.controller.v2.users;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.controller.v2.UserController;
import com.dashboard.oauth.dataTransferObject.user.UserAdminRead;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.service.interfaces.IUserService;
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

@Epic("Users V2")
@Feature("Users V2 API")
@Tag("v2-controller-users")
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
@WithMockUser(authorities = {"dashboard-oauth-user-update", "dashboard-oauth-user-delete",
        "dashboard-oauth-user-block", "dashboard-oauth-user-resend-verification",
        "dashboard-oauth-user-reset-password"})
public abstract class BaseV2UserControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IUserService userService;

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected UserDetailsService springUserDetailsService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

    protected final Faker faker = new Faker();

    protected String testUserId;
    protected String testEmail;
    protected UserAdminRead testUserAdminRead;

    @BeforeEach
    void setUpBase() {
        testUserId = new ObjectId().toHexString();
        testEmail = faker.internet().emailAddress();

        testUserAdminRead = new UserAdminRead();
        testUserAdminRead.setId(testUserId);
        testUserAdminRead.setEmail(testEmail);
        testUserAdminRead.setLocked(false);
    }
}
