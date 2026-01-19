package com.dashboard.oauth.integration;

import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.auth.RefreshTokenRequest;
import com.dashboard.oauth.dataTransferObject.auth.RegisterRequest;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.repository.IRoleRepository;
import com.dashboard.oauth.repository.IUserRepository;
import com.dashboard.oauth.repository.IRefreshTokenRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IUserRepository userRepository;

    private static String testEmail;
    private static String testPassword;
    private static String testRoleId;
    private static String accessToken;
    private static String refreshToken;

    @BeforeAll
    static void initTestData() {
        // Will be set by Faker in tests
    }

    @BeforeEach
    void setUp() {
        if (testEmail == null) {
            testEmail = faker.internet().emailAddress();
            testPassword = faker.internet().password(8, 16, true, true);
        }
    }

    @AfterAll
    void cleanUp(@Autowired IUserRepository userRepository,
                 @Autowired IRoleRepository roleRepository,
                 @Autowired IRefreshTokenRepository refreshTokenRepository) {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @Order(1)
    void shouldCreateRole() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName("ROLE_USER");

        MvcResult result = mockMvc.perform(post("/api/auth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ROLE_USER"))
                .andReturn();

        // Get the role ID from the database
        Role role = roleRepository.findByNameAndAudit_DeletedAtIsNull("ROLE_USER").orElseThrow();
        testRoleId = role.get_id().toHexString();
    }

    @Test
    @Order(2)
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

        // Verify user was created in database
        assertThat(userRepository.findByEmailAndAudit_DeletedAtIsNull(testEmail)).isPresent();
    }

    @Test
    @Order(3)
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
    @Order(4)
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
    @Order(5)
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
    @Order(6)
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
    @Order(7)
    void shouldLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    void shouldFailRefreshAfterLogout() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
