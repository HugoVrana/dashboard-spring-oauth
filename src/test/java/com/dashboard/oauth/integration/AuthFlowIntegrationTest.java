package com.dashboard.oauth.integration;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RefreshTokenRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.repository.IRoleRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IRefreshTokenRepository refreshTokenRepository;

    private String testEmail;
    private String testPassword;
    private String testRoleId;
    private String accessToken;
    private String refreshToken;
    private String testUserId;

    @BeforeAll
    void setUpTestData() {
        testEmail = faker.internet().emailAddress();
        // Password must have: uppercase, lowercase, digit, special char
        testPassword = "Test" + faker.number().digits(4) + "!a";

        // Create role directly in database (role creation endpoint now requires auth)
        // Use unique name to avoid conflicts with other tests
        Role role = new Role();
        role.setName("ROLE_USER_" + System.currentTimeMillis());
        role.setGrants(new ArrayList<>());
        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        role.setAudit(audit);
        role = roleRepository.save(role);
        testRoleId = role.get_id().toHexString();
    }

    @AfterAll
    void cleanUp() {
        // Only delete what this test created - avoid affecting other tests
        if (testUserId != null) {
            refreshTokenRepository.deleteByUserId(testUserId);
            userRepository.deleteById(new org.bson.types.ObjectId(testUserId));
        }
        if (testRoleId != null) {
            roleRepository.deleteById(new org.bson.types.ObjectId(testRoleId));
        }
    }

    @Test
    @Order(1)
    @DisplayName("Register new user")
    void shouldRegisterNewUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRoleId(testRoleId);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(testEmail));

        // Verify user was created in database and capture the ID for cleanup
        var user = userRepository.findByEmailAndAudit_DeletedAtIsNull(testEmail);
        assertThat(user).isPresent();
        testUserId = user.get().get_id().toHexString();
    }

    @Test
    @Order(2)
    @DisplayName("Fail to register duplicate user")
    void shouldFailToRegisterDuplicateUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setRoleId(testRoleId);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("Login and receive tokens")
    void shouldLoginAndReceiveTokens() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = response.getAccessToken();
        refreshToken = response.getRefreshToken();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("Fail login with wrong password")
    void shouldFailLoginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @Order(5)
    @DisplayName("Refresh token")
    void shouldRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);

        assertThat(response.getAccessToken()).isNotBlank();
    }

    @Test
    @Order(6)
    @DisplayName("Logout")
    void shouldLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    @DisplayName("Fail refresh after logout")
    void shouldFailRefreshAfterLogout() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
