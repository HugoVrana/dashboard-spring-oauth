package com.dashboard.oauth.integration.v2;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreate;
import com.dashboard.oauth.dataTransferObject.oauthClient.OAuthClientCreated;
import com.dashboard.oauth.integration.BaseIntegrationTest;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.repository.IOauthClientRepository;
import com.dashboard.oauth.repository.IRoleRepository;
import com.dashboard.oauth.repository.IUserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("V2 OAuth Client Controller")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2OAuthClientControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private IUserRepository userRepository;
    @Autowired private IRoleRepository roleRepository;
    @Autowired private IGrantRepository grantRepository;
    @Autowired private IOauthClientRepository oauthClientRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String adminUserId;
    private String adminRoleId;
    private List<String> adminGrantIds = new ArrayList<>();

    private String testClientId;

    @BeforeAll
    void setUp() throws Exception {
        String email = faker.internet().emailAddress();
        String password = "Test" + faker.number().digits(4) + "!a";

        List<String> grantNames = List.of(
                "dashboard-oauth-client-create",
                "dashboard-oauth-client-delete",
                "dashboard-oauth-client-rotate-secret"
        );

        List<Grant> adminGrants = new ArrayList<>();
        for (String name : grantNames) {
            Grant g = new Grant();
            g.setName(name);
            Audit a = new Audit();
            a.setCreatedAt(Instant.now());
            g.setAudit(a);
            g = grantRepository.save(g);
            adminGrantIds.add(g.get_id().toHexString());
            adminGrants.add(g);
        }

        Role role = new Role();
        role.setName("ROLE_CLIENT_ADMIN_" + System.currentTimeMillis());
        role.setGrants(adminGrants);
        Audit roleAudit = new Audit();
        roleAudit.setCreatedAt(Instant.now());
        role.setAudit(roleAudit);
        role = roleRepository.save(role);
        adminRoleId = role.get_id().toHexString();

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(List.of(role));
        user.setEmailVerified(true);
        Audit userAudit = new Audit();
        userAudit.setCreatedAt(Instant.now());
        user.setAudit(userAudit);
        user = userRepository.save(user);
        adminUserId = user.get_id().toHexString();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = authResponse.getAccessToken();
    }

    @AfterAll
    void cleanUp() {
        if (testClientId != null) oauthClientRepository.deleteById(new ObjectId(testClientId));
        if (adminUserId != null) userRepository.deleteById(new ObjectId(adminUserId));
        if (adminRoleId != null) roleRepository.deleteById(new ObjectId(adminRoleId));
        adminGrantIds.forEach(id -> grantRepository.deleteById(new ObjectId(id)));
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v2/oauthclients/ creates a client and returns secret")
    void createClient_returnsCreatedClient() throws Exception {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("https://app.example.com/callback"));
        request.setAllowedScopes(List.of("openid", "profile"));
        request.setAllowedHosts(List.of("https://app.example.com"));

        MvcResult result = mockMvc.perform(post("/api/v2/oauthclients/")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.redirectUris[0]").value("https://app.example.com/callback"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        OAuthClientCreated created = objectMapper.readValue(body, OAuthClientCreated.class);
        testClientId = created.getId();

        assertThat(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(new ObjectId(testClientId))).isPresent();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v2/oauthclients/ returns 400 for invalid redirect URI")
    void createClient_returns400_forInvalidRedirectUri() throws Exception {
        OAuthClientCreate request = new OAuthClientCreate();
        request.setRedirectUris(List.of("not-a-valid-uri"));
        request.setAllowedScopes(List.of("openid"));
        request.setAllowedHosts(List.of("https://app.example.com"));

        mockMvc.perform(post("/api/v2/oauthclients/")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v2/oauthclients/{id} returns the client")
    void getClient_returnsClient() throws Exception {
        mockMvc.perform(get("/api/v2/oauthclients/" + testClientId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testClientId))
                .andExpect(jsonPath("$.redirectUris[0]").value("https://app.example.com/callback"))
                .andExpect(jsonPath("$.clientSecret").doesNotExist());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v2/oauthclients/{id} returns 404 for unknown id")
    void getClient_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/oauthclients/" + new ObjectId().toHexString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/v2/oauthclients/{id}/secret rotates the client secret")
    void rotateSecret_returnsNewSecret() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/oauthclients/" + testClientId + "/secret")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testClientId))
                .andExpect(jsonPath("$.clientSecret").exists())
                .andReturn();

        OAuthClientCreated rotated = objectMapper.readValue(
                result.getResponse().getContentAsString(), OAuthClientCreated.class);
        assertThat(rotated.getClientSecret()).isNotBlank();
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /api/v2/oauthclients/{id} deletes the client")
    void deleteClient_returns204_andClientIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/v2/oauthclients/" + testClientId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(oauthClientRepository.findBy_idAndAudit_DeletedAtIsNull(new ObjectId(testClientId))).isEmpty();
        testClientId = null;
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /api/v2/oauthclients/{id} returns 404 for unknown id")
    void deleteClient_returns404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/api/v2/oauthclients/" + new ObjectId().toHexString())
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
