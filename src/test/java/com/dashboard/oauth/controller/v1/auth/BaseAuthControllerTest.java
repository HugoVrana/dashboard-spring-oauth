package com.dashboard.oauth.controller.v1.auth;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.Audit;
import com.dashboard.oauth.controller.v1.AuthenticationController;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.interfaces.*;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.ArrayList;

@Epic("Authentication")
@Feature( "Authentication")
@Tag("controller-authentication")
@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
public abstract class BaseAuthControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IAuthenticationService authService;

    @MockitoBean
    protected IDashboardUserDetailService userDetailsService;

    @MockitoBean
    protected IUserService userService;

    @MockitoBean
    protected IRoleService roleService;

    @MockitoBean
    protected IGrantService grantService;

    @MockitoBean
    protected IJwtService jwtService;

    @MockitoBean
    protected IUserInfoMapper userInfoMapper;

    @MockitoBean
    protected IRoleMapper roleMapper;

    @MockitoBean
    protected IGrantMapper grantMapper;

    @MockitoBean
    protected IEmailService emailService;

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected UserDetailsService springUserDetailsService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

    @MockitoBean
    protected IActivityFeedService activityFeedService;

    protected final Faker faker = new Faker();

    protected ObjectId testRoleId;
    protected ObjectId testUserId;
    protected ObjectId testGrantId;

    protected String testEmail;
    protected String testPassword;
    protected String testRoleName;
    protected String testGrantName;
    protected String testGrantDescription;
    protected String testAccessToken;
    protected String testRefreshToken;

    @BeforeEach
    void setUpBase() {
        testRoleId = new ObjectId();
        testUserId = new ObjectId();
        testGrantId = new ObjectId();

        testEmail = faker.internet().emailAddress();
        // Password must have: uppercase, lowercase, digit, special char
        testPassword = "Test" + faker.number().digits(4) + "!a";
        testRoleName = faker.expression("#{letterify 'ROLE_????'}").toUpperCase();
        testGrantName = faker.expression("#{letterify 'GRANT_????'}").toUpperCase();
        testGrantDescription = faker.lorem().sentence();
        testAccessToken = faker.regexify("[a-zA-Z0-9]{32}");
        testRefreshToken = faker.regexify("[a-zA-Z0-9]{32}");
    }

    protected User createTestUser() {
        User user = new User();
        user.set_id(testUserId);
        user.setEmail(testEmail);
        user.setPassword(testPassword);
        user.setRoles(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }

    protected Role createTestRole() {
        Role role = new Role();
        role.set_id(testRoleId);
        role.setName(testRoleName);
        role.setGrants(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        role.setAudit(audit);

        return role;
    }
}
