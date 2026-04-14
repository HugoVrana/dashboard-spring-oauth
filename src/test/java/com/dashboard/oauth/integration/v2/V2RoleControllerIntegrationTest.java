package com.dashboard.oauth.integration.v2;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleUpdate;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("V2 Role Controller")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2RoleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private IRoleRepository roleRepository;
    @Autowired private IGrantRepository grantRepository;
    @Autowired private IUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String adminUserId;
    private String adminRoleId;
    private List<String> adminGrantIds = new ArrayList<>();

    private String testRoleId;
    private String testRoleName;
    private String testRoleNameUpdated;

    // A grant we create to assign/remove from the test role
    private String memberGrantId;

    @BeforeAll
    void setUp() throws Exception {
        testRoleName = faker.expression("#{letterify 'ROLE_????'}").toUpperCase();
        testRoleNameUpdated = faker.expression("#{letterify 'ROLE_????'}").toUpperCase();
        String email = faker.internet().emailAddress();
        String password = "Test" + faker.number().digits(4) + "!a";

        List<String> grantNames = List.of(
                "dashboard-oauth-role-create",
                "dashboard-oauth-role-update",
                "dashboard-oauth-role-delete",
                "dashboard-oauth-role-manage-grants"
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

        // A separate grant to add/remove from the test role
        Grant memberGrant = new Grant();
        memberGrant.setName("MEMBER_GRANT_" + System.currentTimeMillis());
        Audit mga = new Audit();
        mga.setCreatedAt(Instant.now());
        memberGrant.setAudit(mga);
        memberGrant = grantRepository.save(memberGrant);
        memberGrantId = memberGrant.get_id().toHexString();

        Role role = new Role();
        role.setName("ROLE_ROLE_ADMIN_" + System.currentTimeMillis());
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
        if (testRoleId != null) roleRepository.deleteById(new ObjectId(testRoleId));
        if (memberGrantId != null) grantRepository.deleteById(new ObjectId(memberGrantId));
        if (adminUserId != null) userRepository.deleteById(new ObjectId(adminUserId));
        if (adminRoleId != null) roleRepository.deleteById(new ObjectId(adminRoleId));
        adminGrantIds.forEach(id -> grantRepository.deleteById(new ObjectId(id)));
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/v2/role/ returns list of roles")
    void getAllRoles_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v2/role/")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v2/role/ creates a role")
    void createRole_returnsCreatedRole() throws Exception {
        CreateRole request = new CreateRole();
        request.setName(testRoleName);

        MvcResult result = mockMvc.perform(post("/api/v2/role/")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        testRoleId = objectMapper.readTree(body).get("id").asText();

        assertThat(roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName)).isPresent();
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/v2/role/ returns 409 when name already exists")
    void createRole_returnsConflict_whenNameTaken() throws Exception {
        CreateRole request = new CreateRole();
        request.setName(testRoleName);

        mockMvc.perform(post("/api/v2/role/")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v2/role/{id} returns the role")
    void getRoleById_returnsRole() throws Exception {
        mockMvc.perform(get("/api/v2/role/" + testRoleId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testRoleId))
                .andExpect(jsonPath("$.name").value(testRoleName));
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/v2/role/{id} returns 404 for unknown id")
    void getRoleById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/role/" + new ObjectId().toHexString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/v2/role/{id} updates the role name")
    void updateRole_returnsUpdatedRole() throws Exception {
        RoleUpdate update = new RoleUpdate();
        update.setName(testRoleNameUpdated);

        mockMvc.perform(put("/api/v2/role/" + testRoleId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleNameUpdated));

        assertThat(roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleNameUpdated)).isPresent();
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/v2/role/{id}/grants/{grantId} adds grant to role")
    void addGrantToRole_returnsUpdatedRole() throws Exception {
        mockMvc.perform(post("/api/v2/role/" + testRoleId + "/grants/" + memberGrantId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grants[0].id").value(memberGrantId));

        Role role = roleRepository.findById(new ObjectId(testRoleId)).orElseThrow();
        assertThat(role.getGrants()).hasSize(1);
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/v2/role/{id}/grants/{grantId} returns 409 if grant already assigned")
    void addGrantToRole_returnsConflict_whenAlreadyAssigned() throws Exception {
        mockMvc.perform(post("/api/v2/role/" + testRoleId + "/grants/" + memberGrantId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/v2/role/{id}/grants/{grantId} removes grant from role")
    void removeGrantFromRole_returnsUpdatedRole() throws Exception {
        mockMvc.perform(delete("/api/v2/role/" + testRoleId + "/grants/" + memberGrantId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grants").isEmpty());

        Role role = roleRepository.findById(new ObjectId(testRoleId)).orElseThrow();
        assertThat(role.getGrants()).isEmpty();
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/v2/role/{id}/grants/{grantId} returns 404 if grant not assigned")
    void removeGrantFromRole_returns404_whenNotAssigned() throws Exception {
        mockMvc.perform(delete("/api/v2/role/" + testRoleId + "/grants/" + memberGrantId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /api/v2/role/{id} soft-deletes the role")
    void deleteRole_returns204_andRoleIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/v2/role/" + testRoleId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(roleRepository.findRoleBy_idAndAudit_DeletedAtIsNull(new ObjectId(testRoleId))).isEmpty();
        testRoleId = null;
    }

    @Test
    @Order(12)
    @DisplayName("DELETE /api/v2/role/{id} returns 404 for unknown id")
    void deleteRole_returns404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/api/v2/role/" + new ObjectId().toHexString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
