package com.dashboard.oauth.integration;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleGrantRequest;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.model.entities.User;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.repository.IRoleRepository;
import com.dashboard.oauth.repository.IUserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Role grant flow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleGrantIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IGrantRepository grantRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String testRoleId;
    private String testGrantId;
    private String testRoleName;
    private String testGrantName;

    @BeforeAll
    void setUpAuth() throws Exception {
        String testEmail = faker.internet().emailAddress();
        String testPassword = faker.internet().password(8, 16, true, true);
        testRoleName = faker.expression("#{letterify 'ROLE_????'}").toUpperCase();
        testGrantName = faker.expression("#{letterify 'GRANT_????'}").toUpperCase();

        // Create a role for the test user
        Role userRole = new Role();
        userRole.setName("ROLE_ADMIN");
        userRole.setGrants(new ArrayList<>());
        Audit roleAudit = new Audit();
        roleAudit.setCreatedAt(Instant.now());
        userRole.setAudit(roleAudit);
        userRole = roleRepository.save(userRole);

        // Create test user directly in database with encoded password
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode(testPassword));
        user.setRoles(new ArrayList<>());
        user.getRoles().add(userRole);
        Audit userAudit = new Audit();
        userAudit.setCreatedAt(Instant.now());
        user.setAudit(userAudit);
        userRepository.save(user);

        // Login to get access token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = response.getAccessToken();
    }

    @AfterAll
    void cleanUp() {
        grantRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Create role and grant")
    void shouldCreateRole() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        mockMvc.perform(post("/api/role/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName));

        Role role = roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName).orElseThrow();
        testRoleId = role.get_id().toHexString();
    }

    @Test
    @Order(2)
    @DisplayName("Fail to create duplicate role")
    void shouldFailToCreateDuplicateRole() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        mockMvc.perform(post("/api/role/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("Create grant")
    void shouldCreateGrant() throws Exception {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription("Test grant for integration testing");

        mockMvc.perform(post("/api/grant/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testGrantName));

        Grant grant = grantRepository.findByName(testGrantName).orElseThrow();
        testGrantId = grant.get_id().toHexString();
    }

    @Test
    @Order(4)
    @DisplayName("Fail to create grant with duplicate name")
    void shouldFailToCreateDuplicateGrant() throws Exception {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription("Duplicate grant");

        mockMvc.perform(post("/api/grant/")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantCreate)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(5)
    @DisplayName("Add grant to role")
    void shouldAddGrantToRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId);
        request.setGrantId(testGrantId);

        mockMvc.perform(post("/api/role/grant")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName))
                .andExpect(jsonPath("$.grants[0].name").value(testGrantName));

        // Verify in database
        Role role = roleRepository.findById(new org.bson.types.ObjectId(testRoleId)).orElseThrow();
        assertThat(role.getGrants()).hasSize(1);
        assertThat(role.getGrants().getFirst().getName()).isEqualTo(testGrantName);
    }

    @Test
    @Order(6)
    @DisplayName("Fail to add duplicate grant to role")
    void shouldFailToAddDuplicateGrantToRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId);
        request.setGrantId(testGrantId);

        mockMvc.perform(post("/api/role/grant")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(7)
    @DisplayName("Remove grant from role")
    void shouldRemoveGrantFromRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId);
        request.setGrantId(testGrantId);

        mockMvc.perform(delete("/api/role/grant")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        // Verify in database
        Role role = roleRepository.findById(new org.bson.types.ObjectId(testRoleId)).orElseThrow();
        assertThat(role.getGrants()).isEmpty();
    }

    @Test
    @Order(8)
    @DisplayName("Return 0 when removing non-existent grant from role")
    void shouldReturn0WhenRemovingNonExistentGrantFromRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId);
        request.setGrantId(testGrantId);

        mockMvc.perform(delete("/api/role/grant")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
