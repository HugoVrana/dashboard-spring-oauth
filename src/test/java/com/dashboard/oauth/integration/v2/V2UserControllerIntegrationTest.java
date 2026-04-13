package com.dashboard.oauth.integration.v2;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.dataTransferObject.auth.AuthResponse;
import com.dashboard.oauth.dataTransferObject.auth.LoginRequest;
import com.dashboard.oauth.dataTransferObject.user.UserAdminUpdate;
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

@DisplayName("V2 User Controller")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private IUserRepository userRepository;
    @Autowired private IRoleRepository roleRepository;
    @Autowired private IGrantRepository grantRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;
    private String adminUserId;
    private String adminRoleId;
    private List<String> adminGrantIds = new ArrayList<>();

    // A separate user that is the subject of admin operations
    private String targetUserId;
    private String targetUserEmail;
    private String targetUserUpdatedEmail;

    @BeforeAll
    void setUp() throws Exception {
        targetUserEmail = faker.internet().emailAddress();
        targetUserUpdatedEmail = faker.internet().emailAddress();
        String adminEmail = faker.internet().emailAddress();
        String adminPassword = "Test" + faker.number().digits(4) + "!a";

        List<String> grantNames = List.of(
                "dashboard-oauth-user-update",
                "dashboard-oauth-user-delete",
                "dashboard-oauth-user-block",
                "dashboard-oauth-user-resend-verification",
                "dashboard-oauth-user-reset-password"
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

        Role adminRole = new Role();
        adminRole.setName("ROLE_USER_ADMIN_" + System.currentTimeMillis());
        adminRole.setGrants(adminGrants);
        Audit roleAudit = new Audit();
        roleAudit.setCreatedAt(Instant.now());
        adminRole.setAudit(roleAudit);
        adminRole = roleRepository.save(adminRole);
        adminRoleId = adminRole.get_id().toHexString();

        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.setRoles(List.of(adminRole));
        Audit adminAudit = new Audit();
        adminAudit.setCreatedAt(Instant.now());
        adminUser.setAudit(adminAudit);
        adminUser = userRepository.save(adminUser);
        adminUserId = adminUser.get_id().toHexString();

        // Create the target user to be managed
        User targetUser = new User();
        targetUser.setEmail(targetUserEmail);
        targetUser.setPassword(passwordEncoder.encode("Target1!pass"));
        targetUser.setRoles(new ArrayList<>());
        Audit targetAudit = new Audit();
        targetAudit.setCreatedAt(Instant.now());
        targetUser.setAudit(targetAudit);
        targetUser = userRepository.save(targetUser);
        targetUserId = targetUser.get_id().toHexString();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(adminEmail);
        loginRequest.setPassword(adminPassword);

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
        if (targetUserId != null) userRepository.deleteById(new ObjectId(targetUserId));
        if (adminUserId != null) userRepository.deleteById(new ObjectId(adminUserId));
        if (adminRoleId != null) roleRepository.deleteById(new ObjectId(adminRoleId));
        adminGrantIds.forEach(id -> grantRepository.deleteById(new ObjectId(id)));
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/v2/user/ returns list of users")
    void getAllUsers_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v2/user/")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.email == '" + targetUserEmail + "')]").exists());
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/v2/user/search returns matching users")
    void searchUsers_returnsMatchingUsers() throws Exception {
        String query = targetUserEmail.split("@")[0];

        mockMvc.perform(get("/api/v2/user/search")
                        .param("q", query)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v2/user/{id} returns user by ID")
    void getUserById_returnsUser() throws Exception {
        mockMvc.perform(get("/api/v2/user/" + targetUserId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUserId))
                .andExpect(jsonPath("$.email").value(targetUserEmail));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v2/user/{id} returns 404 for unknown id")
    void getUserById_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v2/user/" + new ObjectId().toHexString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/v2/user/{id} updates the user email")
    void updateUser_returnsUpdatedUser() throws Exception {
        UserAdminUpdate update = new UserAdminUpdate();
        update.setEmail(targetUserUpdatedEmail);

        mockMvc.perform(put("/api/v2/user/" + targetUserId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(targetUserUpdatedEmail));

        assertThat(userRepository.findByEmailAndAudit_DeletedAtIsNull(targetUserUpdatedEmail)).isPresent();
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/v2/user/{id}/block blocks the user")
    void blockUser_returns204() throws Exception {
        mockMvc.perform(post("/api/v2/user/" + targetUserId + "/block")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(new ObjectId(targetUserId)).orElseThrow();
        assertThat(user.isLocked()).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/v2/user/{id}/block returns 409 when already blocked")
    void blockUser_returnsConflict_whenAlreadyBlocked() throws Exception {
        mockMvc.perform(post("/api/v2/user/" + targetUserId + "/block")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/v2/user/{id}/unblock unblocks the user")
    void unblockUser_returns204() throws Exception {
        mockMvc.perform(post("/api/v2/user/" + targetUserId + "/unblock")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        User user = userRepository.getUserBy_idAndAudit_DeletedAtIsNull(new ObjectId(targetUserId)).orElseThrow();
        assertThat(user.isLocked()).isFalse();
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/v2/user/{id}/unblock returns 409 when not blocked")
    void unblockUser_returnsConflict_whenNotBlocked() throws Exception {
        mockMvc.perform(post("/api/v2/user/" + targetUserId + "/unblock")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/v2/user/{id} soft-deletes the user")
    void deleteUser_returns204_andUserIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/v2/user/" + targetUserId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.getUserBy_idAndAudit_DeletedAtIsNull(new ObjectId(targetUserId))).isEmpty();
        targetUserId = null;
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /api/v2/user/{id} returns 404 for unknown id")
    void deleteUser_returns404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/api/v2/user/" + new ObjectId().toHexString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
