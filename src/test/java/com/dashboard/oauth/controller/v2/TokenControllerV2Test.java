package com.dashboard.oauth.controller.v2;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.InvalidRequestException;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.user.UserInfoRead;
import com.dashboard.oauth.dataTransferObject.v2.IntrospectionResponse;
import com.dashboard.oauth.dataTransferObject.v2.SubmitAuthorizeResult;
import com.dashboard.oauth.environment.Oauth2Properties;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.model.entities.AuthorizationCode;
import com.dashboard.oauth.model.entities.AuthorizationRequest;
import com.dashboard.oauth.model.entities.TotpConfig;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.service.interfaces.IActivityFeedService;
import com.dashboard.oauth.service.interfaces.IAuthenticationService;
import com.dashboard.oauth.service.interfaces.IAuthorizationService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    private IAuthorizationService authorizationService;

    @MockitoBean
    private IAuthenticationService authenticationService;

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
    private static final String LOGIN_URL = "https://app.example.com/login";
    private static final String REDIRECT_URI = "https://app.example.com/callback";
    private static final String CLIENT_ID = "test-client";
    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    private ObjectId testUserId;

    @BeforeEach
    void setUp() {
        testUserId = new ObjectId();
        when(oauth2Properties.getSecret()).thenReturn(SERVICE_SECRET);
        when(oauth2Properties.getLoginUrl()).thenReturn(LOGIN_URL);
    }

    // -------------------------------------------------------------------------
    // GET /v2/oauth2/authorize
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /v2/oauth2/authorize with valid params → 302 redirect to login page")
    void authorize_get_success() throws Exception {
        AuthorizationRequest request = buildAuthorizationRequest(null);
        when(authorizationService.createAuthorizationRequest(
                eq(CLIENT_ID), eq(REDIRECT_URI), eq(CODE_CHALLENGE), eq("S256"), any(), any()))
                .thenReturn(request);

        mockMvc.perform(get("/v2/oauth2/authorize")
                        .param("response_type", "code")
                        .param("client_id", CLIENT_ID)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code_challenge", CODE_CHALLENGE)
                        .param("code_challenge_method", "S256"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        LOGIN_URL + "?request_id=" + request.getId().toHexString()));
    }

    @Test
    @DisplayName("GET /v2/oauth2/authorize with unsupported response_type → 302 error redirect")
    void authorize_get_unsupportedResponseType() throws Exception {
        mockMvc.perform(get("/v2/oauth2/authorize")
                        .param("response_type", "token")
                        .param("client_id", CLIENT_ID)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code_challenge", CODE_CHALLENGE)
                        .param("code_challenge_method", "S256"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("error=unsupported_response_type")));
    }

    @Test
    @DisplayName("GET /v2/oauth2/authorize with unknown client → 302 error redirect")
    void authorize_get_unknownClient() throws Exception {
        when(authorizationService.createAuthorizationRequest(any(), any(), any(), any(), any(), any()))
                .thenThrow(new InvalidRequestException("Unknown client_id"));

        mockMvc.perform(get("/v2/oauth2/authorize")
                        .param("response_type", "code")
                        .param("client_id", "unknown")
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code_challenge", CODE_CHALLENGE)
                        .param("code_challenge_method", "S256"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("error=invalid_request")));
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/authorize
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /v2/oauth2/authorize with valid credentials and no 2FA → 302 redirect with code")
    void authorize_post_success_no2fa() throws Exception {
        AuthorizationRequest authRequest = buildAuthorizationRequest(null);
        AuthorizationCode code = buildAuthorizationCode("abc123", null);

        when(authorizationService.getAuthorizationRequest(authRequest.getId().toHexString()))
                .thenReturn(authRequest);
        when(authorizationService.submitAuthorize(eq(authRequest), eq("user@example.com"), eq("Password1!")))
                .thenReturn(new SubmitAuthorizeResult(false, null, code));

        mockMvc.perform(post("/v2/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("request_id", authRequest.getId().toHexString())
                        .param("username", "user@example.com")
                        .param("password", "Password1!"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        REDIRECT_URI + "?code=abc123"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/authorize with 2FA enabled → 200 mfa_required")
    void authorize_post_mfaRequired() throws Exception {
        AuthorizationRequest authRequest = buildAuthorizationRequest(null);

        when(authorizationService.getAuthorizationRequest(authRequest.getId().toHexString()))
                .thenReturn(authRequest);
        when(authorizationService.submitAuthorize(eq(authRequest), eq("user@example.com"), eq("Password1!")))
                .thenReturn(new SubmitAuthorizeResult(true, "mfa-token-xyz", null));

        mockMvc.perform(post("/v2/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("request_id", authRequest.getId().toHexString())
                        .param("username", "user@example.com")
                        .param("password", "Password1!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfa_required").value(true))
                .andExpect(jsonPath("$.mfa_token").value("mfa-token-xyz"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/authorize with bad credentials → 302 error redirect")
    void authorize_post_badCredentials() throws Exception {
        AuthorizationRequest authRequest = buildAuthorizationRequest(null);

        when(authorizationService.getAuthorizationRequest(authRequest.getId().toHexString()))
                .thenReturn(authRequest);
        when(authorizationService.submitAuthorize(eq(authRequest), eq("user@example.com"), eq("wrong")))
                .thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/v2/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("request_id", authRequest.getId().toHexString())
                        .param("username", "user@example.com")
                        .param("password", "wrong"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("error=access_denied")));
    }

    @Test
    @DisplayName("POST /v2/oauth2/authorize with invalid request_id → 400 invalid_request")
    void authorize_post_invalidRequestId() throws Exception {
        when(authorizationService.getAuthorizationRequest(any()))
                .thenThrow(new InvalidRequestException("Authorization request not found or already used"));

        mockMvc.perform(post("/v2/oauth2/authorize")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("request_id", new ObjectId().toHexString())
                        .param("username", "user@example.com")
                        .param("password", "Password1!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/authorize/mfa
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /v2/oauth2/authorize/mfa with valid TOTP → 302 redirect with code")
    void authorizeMfa_success() throws Exception {
        AuthorizationCode code = buildAuthorizationCode("code-xyz", "my-state");

        when(authorizationService.exchangeMfaToken("valid-mfa-token", "123456"))
                .thenReturn(code);

        mockMvc.perform(post("/v2/oauth2/authorize/mfa")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("mfa_token", "valid-mfa-token")
                        .param("totp_code", "123456"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        REDIRECT_URI + "?code=code-xyz&state=my-state"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/authorize/mfa with wrong TOTP code → 401 invalid_grant")
    void authorizeMfa_wrongTotpCode() throws Exception {
        when(authorizationService.exchangeMfaToken("valid-mfa-token", "000000"))
                .thenReturn(null);

        mockMvc.perform(post("/v2/oauth2/authorize/mfa")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("mfa_token", "valid-mfa-token")
                        .param("totp_code", "000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/authorize/mfa with expired mfa_token → 400 invalid_request")
    void authorizeMfa_expiredToken() throws Exception {
        when(authorizationService.exchangeMfaToken("expired-token", "123456"))
                .thenThrow(new InvalidRequestException("Invalid or expired MFA token"));

        mockMvc.perform(post("/v2/oauth2/authorize/mfa")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("mfa_token", "expired-token")
                        .param("totp_code", "123456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /v2/oauth2/token with authorization_code grant → 200 with token fields")
    void token_authorizationCodeGrant_success() throws Exception {
        when(authorizationService.exchangeCode("my-code", "my-verifier", CLIENT_ID, REDIRECT_URI, null))
                .thenReturn(buildAuthResponse());

        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", "my-code")
                        .param("code_verifier", "my-verifier")
                        .param("client_id", CLIENT_ID)
                        .param("redirect_uri", REDIRECT_URI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(86400L))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with authorization_code missing params → 400 invalid_request")
    void token_authorizationCodeGrant_missingParams() throws Exception {
        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", "my-code"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with invalid code → 400 invalid_grant")
    void token_authorizationCodeGrant_invalidCode() throws Exception {
        when(authorizationService.exchangeCode(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidRequestException("Invalid or expired authorization code"));

        mockMvc.perform(post("/v2/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", "bad-code")
                        .param("code_verifier", "verifier")
                        .param("client_id", CLIENT_ID)
                        .param("redirect_uri", REDIRECT_URI))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    @DisplayName("POST /v2/oauth2/token with refresh_token grant → 200")
    void token_refreshTokenGrant_success() throws Exception {
        when(authenticationService.refreshToken("my-refresh-token")).thenReturn(buildAuthResponse());

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

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/revoke
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /v2/oauth2/revoke → 200 regardless of token validity")
    void revoke_alwaysReturns200() throws Exception {
        doNothing().when(authenticationService).revokeToken(any());

        mockMvc.perform(post("/v2/oauth2/revoke")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", "any-token"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // POST /v2/oauth2/introspect
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /v2/oauth2/introspect with valid token and correct Basic Auth → 200 active=true")
    void introspect_validToken_active() throws Exception {
        IntrospectionResponse introspectionResponse = new IntrospectionResponse();
        introspectionResponse.setActive(true);
        introspectionResponse.setSub("test@example.com");
        introspectionResponse.setScope("READ");
        introspectionResponse.setExp(Instant.now().plusSeconds(3600).getEpochSecond());

        when(authorizationService.validateClientSecret(BASIC_AUTH)).thenReturn(true);
        when(authorizationService.introspect("valid-jwt")).thenReturn(introspectionResponse);

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
        IntrospectionResponse inactive = new IntrospectionResponse();
        inactive.setActive(false);

        when(authorizationService.validateClientSecret(BASIC_AUTH)).thenReturn(true);
        when(authorizationService.introspect("bad-jwt")).thenReturn(inactive);

        mockMvc.perform(post("/v2/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Authorization", BASIC_AUTH)
                        .param("token", "bad-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AuthResponse buildAuthResponse() {
        AuthResponse r = new AuthResponse();
        r.setAccessToken("access-token");
        r.setRefreshToken("refresh-token");
        r.setTokenType("Bearer");
        r.setExpiresIn(86400000L);
        r.setUser(new UserInfoRead());
        return r;
    }

    private AuthorizationRequest buildAuthorizationRequest(String state) {
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        AuthorizationRequest request = AuthorizationRequest.builder()
                .id(new ObjectId())
                .clientId(CLIENT_ID)
                .redirectUri(REDIRECT_URI)
                .codeChallenge(CODE_CHALLENGE)
                .codeChallengeMethod("S256")
                .state(state)
                .used(false)
                .expiryDate(Instant.now().plusSeconds(600))
                .audit(audit)
                .build();
        return request;
    }

    private AuthorizationCode buildAuthorizationCode(String code, String state) {
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        return AuthorizationCode.builder()
                .id(new ObjectId())
                .code(code)
                .clientId(CLIENT_ID)
                .userId(testUserId.toHexString())
                .redirectUri(REDIRECT_URI)
                .codeChallenge(CODE_CHALLENGE)
                .codeChallengeMethod("S256")
                .state(state)
                .used(false)
                .expiryDate(Instant.now().plusSeconds(600))
                .audit(audit)
                .build();
    }

    private User createTestUser(boolean totpEnabled) {
        User user = new User();
        user.set_id(testUserId);
        user.setEmail("user@example.com");
        user.setRoles(new ArrayList<>());
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        user.setAudit(audit);
        if (totpEnabled) {
            TotpConfig totp = new TotpConfig();
            totp.setEnabled(true);
            user.setTwoFactorConfig(totp);
        }
        return user;
    }

}
