package com.dashboard.oauth.integration.v2;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.grant.GrantUpdate;
import com.dashboard.oauth.integration.BaseIntegrationTest;
import com.dashboard.oauth.model.entities.auth.Grant;
import com.dashboard.oauth.model.entities.auth.Role;
import com.dashboard.oauth.model.entities.user.User;
import com.dashboard.oauth.repository.IGrantRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("V2 Grant Controller")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2GrantControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private IGrantRepository grantRepository;
    @Autowired private IRoleRepository roleRepository;
    @Autowired private IUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String adminUserId;
    private String adminRoleId;
    private List<String> adminGrantIds = new ArrayList<>();

    private String testGrantId;
    private String testGrantName;
    private String testGrantName2;

    @BeforeAll
    void setUp() throws Exception {
        testGrantName = faker.expression("#{letterify 'GRANT_????'}").toUpperCase();
        testGrantName2 = faker.expression("#{letterify 'GRANT_????'}").toUpperCase();
        String email = faker.internet().emailAddress();
        String password = "Test" + faker.number().digits(4) + "!a";

        List<String> grantNames = List.of(
                "dashboard-oauth-grant-create",
                "dashboard-oauth-grant-update",
                "dashboard-oauth-grant-delete"
        );

        List<Grant> grants = new ArrayList<>();
        for (String name : grantNames) {
            Grant g = new Grant();
            g.setName(name);
            Audit a = new Audit();
            a.setCreatedAt(Instant.now());
            g.setAudit(a);
            g = grantRepository.save(g);
            adminGrantIds.add(g.get_id().toHexString());
            grants.add(g);
        }

        Role role = new Role();
        role.setName("ROLE_GRANT_ADMIN_" + System.currentTimeMillis());
        role.setGrants(grants);
        Audit roleAudit = new Audit();
        roleAudit.setCreatedAt(Instant.now());
        role.setAudit(roleAudit);
        role = roleRepository.save(role);
        adminRoleId = role.get_id().toHexString();

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(List.of(role));
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
        if (testGrantId != null) grantRepository.deleteById(new ObjectId(testGrantId));
        if (adminUserId != null) userRepository.deleteById(new ObjectId(adminUserId));
        if (adminRoleId != null) roleRepository.deleteById(new ObjectId(adminRoleId));
        adminGrantIds.forEach(id -> grantRepository.deleteById(new ObjectId(id)));
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/v2/grant/ returns list of grants")
    void getAllGrants_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v2/grant/")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v2/grant/ creates a grant")
    void createGrant_returnsCreatedGrant() throws Exception {
        GrantCreate request = new GrantCreate();
        request.setName(testGrantName);
        request.setDescription("Integration test grant");

        MvcResult result = mockMvc.perform(post("/api/v2/grant/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testGrantName))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        testGrantId = objectMapper.readTree(body).get("id").asText();

        assertThat(grantRepository.getGrantByNameAndAudit_DeletedAtIsNull(testGrantName)).isPresent();
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/v2/grant/ returns 409 when name already exists")
    void createGrant_returnsConflict_whenNameTaken() throws Exception {
        GrantCreate request = new GrantCreate();
        request.setName(testGrantName);
        request.setDescription("Duplicate");

        mockMvc.perform(post("/api/v2/grant/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v2/grant/{id} returns the grant")
    void getGrantById_returnsGrant() throws Exception {
        mockMvc.perform(get("/api/v2/grant/" + testGrantId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testGrantId))
                .andExpect(jsonPath("$.name").value(testGrantName));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v2/grant/{id} returns 404 for unknown id")
    void getGrantById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/grant/" + new ObjectId().toHexString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/v2/grant/{id} updates the grant")
    void updateGrant_returnsUpdatedGrant() throws Exception {
        GrantUpdate update = new GrantUpdate();
        update.setName(testGrantName2);
        update.setDescription("Updated description");

        mockMvc.perform(put("/api/v2/grant/" + testGrantId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testGrantName2));

        assertThat(grantRepository.getGrantByNameAndAudit_DeletedAtIsNull(testGrantName2)).isPresent();
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /api/v2/grant/{id} soft-deletes the grant")
    void deleteGrant_returns204_andGrantIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/v2/grant/" + testGrantId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(grantRepository.getGrantByNameAndAudit_DeletedAtIsNull(testGrantName2)).isEmpty();
        testGrantId = null;
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/v2/grant/{id} returns 404 for unknown id")
    void deleteGrant_returns404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/api/v2/grant/" + new ObjectId().toHexString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    @DisplayName("Requests without token return 401")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v2/grant/"))
                .andExpect(status().isUnauthorized());
    }
}
