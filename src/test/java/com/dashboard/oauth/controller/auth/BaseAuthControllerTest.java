package com.dashboard.oauth.controller.auth;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.Audit;
import com.dashboard.oauth.controller.AuthenticationController;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.mapper.interfaces.IGrantMapper;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.mapper.interfaces.IUserInfoMapper;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.interfaces.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.ArrayList;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
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
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected UserDetailsService springUserDetailsService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

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
        testPassword = faker.internet().password(8, 16, true, true);
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
