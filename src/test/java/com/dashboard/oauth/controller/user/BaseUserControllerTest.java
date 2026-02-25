package com.dashboard.oauth.controller.user;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.Audit;
import com.dashboard.oauth.controller.UserController;
import com.dashboard.oauth.controller.config.TestConfig;
import com.dashboard.oauth.environment.R2Properties;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.UserDetailsImpl;
import com.dashboard.oauth.service.interfaces.IR2Service;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.ArrayList;

@Epic("User")
@Feature("Profile Picture")
@Tag("controller-user")
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
public abstract class BaseUserControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IUserService userService;

    @MockitoBean
    protected IR2Service r2Service;

    @MockitoBean
    protected R2Properties r2Properties;

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected UserDetailsService springUserDetailsService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

    protected final Faker faker = new Faker();

    protected ObjectId testUserId;
    protected String testEmail;
    protected User testUser;
    protected Authentication testAuthentication;

    @BeforeEach
    void setUpBase() {
        testUserId = new ObjectId();
        testEmail = faker.internet().emailAddress();
        testUser = createTestUser();
        testAuthentication = createTestAuthentication(testUser);
    }

    protected Authentication createTestAuthentication(User user) {
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    protected User createTestUser() {
        User user = new User();
        user.set_id(testUserId);
        user.setEmail(testEmail);
        user.setPassword(faker.internet().password());
        user.setRoles(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }
}
