package com.dashboard.oauth.controller.v2;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.Audit;
import com.dashboard.oauth.controller.config.TestConfig;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.environment.Oauth2Properties;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IGrantService;
import com.dashboard.oauth.service.interfaces.IJwtService;
import com.dashboard.oauth.service.interfaces.IUserService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.jsonwebtoken.Claims;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
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

@Epic("Token V2")
@Feature("Token V2")
@Tag("controller-tokens-v2")
@WebMvcTest(TokenController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
class TokenControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAuthenticationService authenticationService;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private IJwtService jwtService;

    @MockitoBean
    private IGrantService grantService;

    @MockitoBean
    private Oauth2Properties oauth2Properties;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private GrafanaHttpClient grafanaHttpClient;

    @MockitoBean
    private IActivityFeedService activityFeedService;

    private static final String SERVICE_SECRET = "test-service-secret";
    private static final String BASIC_AUTH = "Basic " + Base64.getEncoder()
            .encodeToString(("client:" + SERVICE_SECRET).getBytes(StandardCharsets.UTF_8));

    private ObjectId testUserId;

    @BeforeEach
    void setUp() {
        testUserId = new ObjectId();
        when(oauth2Properties.getSecret()).thenReturn(SERVICE_SECRET);
    }

    // --- /token endpoint ---

    @Test
    @DisplayName("POST /v2/oauth2/token with password grant → 200 with token fields")
    void token_passwordGrant_success() throws Exception {
        AuthResponse authResponse = buildAuthResponse();
        when(authenticationService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", "test@example.com")
                        .param("password", "Test123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(86400L))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with bad credentials → 401 invalid_grant")
    void token_passwordGrant_badCredentials() throws Exception {
        when(authenticationService.login(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", "test@example.com")
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_grant"))
                .andExpect(jsonPath("$.error_description").value("Invalid credentials"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with locked account → 401 invalid_grant")
    void token_passwordGrant_lockedAccount() throws Exception {
        when(authenticationService.login(any())).thenThrow(new LockedException("locked"));

        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", "test@example.com")
                        .param("password", "Test123!"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_grant"))
                .andExpect(jsonPath("$.error_description").value("Account is locked"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with missing username/password → 400 invalid_request")
    void token_passwordGrant_missingParams() throws Exception {
        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("username and password are required"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with refresh_token grant → 200")
    void token_refreshTokenGrant_success() throws Exception {
        AuthResponse authResponse = buildAuthResponse();
        when(authenticationService.refreshToken("my-refresh-token")).thenReturn(authResponse);

        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", "my-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with missing refresh_token → 400 invalid_request")
    void token_refreshTokenGrant_missingParam() throws Exception {
        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "refresh_token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("refresh_token is required"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with invalid refresh_token → 400 invalid_grant")
    void token_refreshTokenGrant_invalid() throws Exception {
        when(authenticationService.refreshToken("bad-token")).thenThrow(new RuntimeException("expired"));

        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", "bad-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"))
                .andExpect(jsonPath("$.error_description").value("Invalid or expired refresh token"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with unsupported grant type → 400 unsupported_grant_type")
    void token_unsupportedGrantType() throws Exception {
        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_grant_type"));
    }

    // --- /introspect endpoint ---

    @Test
    @DisplayName("POST /v2/oauth2/introspect with valid token and correct Basic Auth → 200 active=true")
    void introspect_validToken_active() throws Exception {
        User user = createTestUser();
        Grant grant = createTestGrant("READ");
        Date futureExpiration = Date.from(Instant.now().plusSeconds(3600));

        when(jwtService.extractClaim(eq("valid-jwt"), any(Function.class)))
                .thenReturn(List.of("READ"))
                .thenReturn(futureExpiration);
        when(jwtService.extractUsername("valid-jwt")).thenReturn("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(grantService.getGrantByName("READ")).thenReturn(Optional.of(grant));

        mockMvc.perform(post("/v2/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", BASIC_AUTH)
                        .param("token", "valid-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.sub").value("test@example.com"))
                .andExpect(jsonPath("$.scope").value("READ"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/introspect with wrong secret → 401")
    void introspect_wrongSecret() throws Exception {
        String wrongAuth = "Basic " + Base64.getEncoder()
                .encodeToString("client:wrong-secret".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/v2/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", wrongAuth)
                        .param("token", "any-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v2/oauth2/introspect with no Authorization header → 401")
    void introspect_missingAuth() throws Exception {
        mockMvc.perform(post("/v2/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", "any-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v2/oauth2/introspect with invalid JWT → 200 active=false")
    void introspect_invalidJwt() throws Exception {
        when(jwtService.extractClaim(eq("bad-jwt"), any(Function.class)))
                .thenThrow(new JwtException("invalid"));

        mockMvc.perform(post("/v2/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", BASIC_AUTH)
                        .param("token", "bad-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // --- Helpers ---

    private AuthResponse buildAuthResponse() {
        AuthResponse r = new AuthResponse();
        r.setAccessToken("access-token");
        r.setRefreshToken("refresh-token");
        r.setTokenType("Bearer");
        r.setExpiresIn(86400000L);
        r.setUser(new UserInfoRead());
        return r;
    }

    private User createTestUser() {
        User user = new User();
        user.set_id(testUserId);
        user.setEmail("test@example.com");
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
