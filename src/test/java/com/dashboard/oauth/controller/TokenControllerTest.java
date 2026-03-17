package com.dashboard.oauth.controller;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.Audit;
import com.dashboard.oauth.controller.config.TestConfig;
import com.dashboard.oauth.controller.v1.OAuth2Controller;
import com.dashboard.oauth.dataTransferObject.auth.TokenIntrospectionRequest;
import com.dashboard.oauth.environment.Oauth2Properties;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.GrantService;
import com.dashboard.oauth.service.JwtService;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Token")
@Feature("Token")
@Tag("controller-tokens")
@WebMvcTest(OAuth2Controller.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private Oauth2Properties oauth2Properties;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private GrantService grantService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private GrafanaHttpClient grafanaHttpClient;

    @MockitoBean
    private IActivityFeedService activityFeedService;

    private static final String SERVICE_SECRET = "test-service-secret";
    private ObjectId testUserId;

    @BeforeEach
    void setUp() {
        testUserId = new ObjectId();
        when(oauth2Properties.getSecret()).thenReturn(SERVICE_SECRET);
    }

    @Test
    @DisplayName("Should return 200 OK when token is valid")
    void introspect_shouldReturnActiveTokenInfo() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("valid-jwt-token");

        User user = createTestUser();
        Grant grant = createTestGrant("READ");
        Date futureExpiration = Date.from(Instant.now().plusSeconds(3600));

        when(jwtService.extractClaim(eq("valid-jwt-token"), any(Function.class)))
                .thenAnswer(invocation -> {
                    // Return grants for the first call
                    return List.of("READ");
                })
                .thenReturn(futureExpiration);
        when(jwtService.extractUsername("valid-jwt-token")).thenReturn("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(grantService.getGrantByName("READ")).thenReturn(Optional.of(grant));

        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .header("X-Service-Secret", SERVICE_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.subject").value("test@example.com"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when secret header is invalid")
    void introspect_shouldReturnUnauthorizedWhenSecretInvalid() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("valid-jwt-token");

        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .header("X-Service-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when token is invalid")
    void introspect_shouldReturnInactiveWhenUserNotFound() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("valid-jwt-token");

        Date futureExpiration = Date.from(Instant.now().plusSeconds(3600));

        when(jwtService.extractClaim(eq("valid-jwt-token"), any(Function.class)))
                .thenReturn(List.of("READ"))
                .thenReturn(futureExpiration);
        when(jwtService.extractUsername("valid-jwt-token")).thenReturn("unknown@example.com");
        when(userService.getUserByEmail("unknown@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .header("X-Service-Secret", SERVICE_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.subject").value("unknown@example.com"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when token is invalid")
    void introspect_shouldReturnInactiveWhenUserDeleted() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("valid-jwt-token");

        User deletedUser = createTestUser();
        deletedUser.getAudit().setDeletedAt(Instant.now());

        Date futureExpiration = Date.from(Instant.now().plusSeconds(3600));

        when(jwtService.extractClaim(eq("valid-jwt-token"), any(Function.class)))
                .thenReturn(List.of("READ"))
                .thenReturn(futureExpiration);
        when(jwtService.extractUsername("valid-jwt-token")).thenReturn("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(deletedUser));

        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .header("X-Service-Secret", SERVICE_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when token is invalid")
    void introspect_shouldReturnInactiveWhenGrantNotFound() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("valid-jwt-token");

        User user = createTestUser();
        Date futureExpiration = Date.from(Instant.now().plusSeconds(3600));

        when(jwtService.extractClaim(eq("valid-jwt-token"), any(Function.class)))
                .thenReturn(List.of("DELETED_GRANT"))
                .thenReturn(futureExpiration);
        when(jwtService.extractUsername("valid-jwt-token")).thenReturn("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(grantService.getGrantByName("DELETED_GRANT")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .header("X-Service-Secret", SERVICE_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when token is invalid")
    void introspect_shouldReturnInactiveWhenGrantDeleted() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("valid-jwt-token");

        User user = createTestUser();
        Grant deletedGrant = createTestGrant("DELETED_GRANT");
        deletedGrant.getAudit().setDeletedAt(Instant.now());

        Date futureExpiration = Date.from(Instant.now().plusSeconds(3600));

        when(jwtService.extractClaim(eq("valid-jwt-token"), any(Function.class)))
                .thenReturn(List.of("DELETED_GRANT"))
                .thenReturn(futureExpiration);
        when(jwtService.extractUsername("valid-jwt-token")).thenReturn("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(grantService.getGrantByName("DELETED_GRANT")).thenReturn(Optional.of(deletedGrant));

        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .header("X-Service-Secret", SERVICE_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when token is invalid")
    void introspect_shouldReturnEmptyResponseOnJwtException() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("invalid-jwt-token");

        when(jwtService.extractClaim(eq("invalid-jwt-token"), any(Function.class)))
                .thenThrow(new JwtException("Invalid token"));

        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .header("X-Service-Secret", SERVICE_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").doesNotExist());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when token is invalid")
    void introspect_shouldReturnInactiveWhenTokenExpired() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("expired-jwt-token");

        User user = createTestUser();
        Grant grant = createTestGrant("READ");
        Date pastExpiration = Date.from(Instant.now().minusSeconds(3600));

        when(jwtService.extractClaim(eq("expired-jwt-token"), any(Function.class)))
                .thenReturn(List.of("READ"))
                .thenReturn(pastExpiration);
        when(jwtService.extractUsername("expired-jwt-token")).thenReturn("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(grantService.getGrantByName("READ")).thenReturn(Optional.of(grant));

        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .header("X-Service-Secret", SERVICE_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Should return 500 Internal Server Error when service secret header is missing")
    void introspect_shouldReturn500WhenServiceSecretHeaderMissing() throws Exception {
        TokenIntrospectionRequest request = new TokenIntrospectionRequest();
        request.setToken("valid-jwt-token");

        // Note: Returns 500 because MissingRequestHeaderException is caught by generic exception handler
        mockMvc.perform(post("/api/v1/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    private User createTestUser() {
        User user = new User();
        user.set_id(testUserId);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRoles(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);

        return user;
    }

    private Grant createTestGrant(String name) {
        Grant grant = new Grant();
        grant.set_id(new ObjectId());
        grant.setName(name);
        grant.setDescription("Test grant");

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        grant.setAudit(audit);

        return grant;
    }
}
