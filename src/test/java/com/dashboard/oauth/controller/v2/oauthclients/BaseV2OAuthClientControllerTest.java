package com.dashboard.oauth.controller.v2.oauthclients;

import com.dashboard.common.logging.GrafanaHttpClient;
import com.dashboard.oauth.controller.v1.config.TestConfig;
import com.dashboard.oauth.controller.v2.OAuthClientController;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientRead;
import com.dashboard.oauth.filter.JwtAuthFilter;
import com.dashboard.oauth.service.interfaces.IOAuthClientService;
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

import java.util.List;
import java.util.UUID;

@Epic("OAuth Clients V2")
@Feature("OAuth Clients V2 API")
@Tag("v2-controller-oauthclients")
@WebMvcTest(OAuthClientController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("spring-context")
@WithMockUser(authorities = {"dashboard-oauth-client-create", "dashboard-oauth-client-delete", "dashboard-oauth-client-rotate-secret"})
public abstract class BaseV2OAuthClientControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected IOAuthClientService oAuthClientService;

    @MockitoBean
    protected JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    protected UserDetailsService springUserDetailsService;

    @MockitoBean
    protected GrafanaHttpClient grafanaHttpClient;

    protected final Faker faker = new Faker();

    protected String testClientId;
    protected String testClientSecret;
    protected OAuthClientRead testClientRead;
    protected OAuthClientCreated testClientCreated;

    @BeforeEach
    void setUpBase() {
        testClientId = new ObjectId().toHexString();
        testClientSecret = UUID.randomUUID().toString();

        testClientRead = new OAuthClientRead();
        testClientRead.setId(testClientId);
        testClientRead.setRedirectUris(List.of("https://app.example.com/callback"));
        testClientRead.setAllowedScopes(List.of("openid", "profile"));

        testClientCreated = new OAuthClientCreated();
        testClientCreated.setId(testClientId);
        testClientCreated.setRedirectUris(List.of("https://app.example.com/callback"));
        testClientCreated.setAllowedScopes(List.of("openid", "profile"));
        testClientCreated.setClientSecret(testClientSecret);
    }
}
