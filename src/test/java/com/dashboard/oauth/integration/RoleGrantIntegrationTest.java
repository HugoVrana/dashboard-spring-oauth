package com.dashboard.oauth.integration;

import com.dashboard.oauth.dataTransferObject.grant.GrantCreate;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleGrantRequest;
import com.dashboard.oauth.model.entities.Grant;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.repository.IGrantRepository;
import com.dashboard.oauth.repository.IRoleRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleGrantIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IGrantRepository grantRepository;

    private static String testRoleId;
    private static String testGrantId;
    private static String testRoleName;
    private static String testGrantName;

    @BeforeEach
    void setUp() {
        if (testRoleName == null) {
            testRoleName = faker.expression("#{letterify 'ROLE_????'}").toUpperCase();
            testGrantName = faker.expression("#{letterify 'GRANT_????'}").toUpperCase();
        }
    }

    @AfterAll
    void cleanUp(@Autowired IRoleRepository roleRepository,
                 @Autowired IGrantRepository grantRepository) {
        roleRepository.deleteAll();
        grantRepository.deleteAll();
    }

    @Test
    @Order(1)
    void shouldCreateRole() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        mockMvc.perform(post("/api/auth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testRoleName));

        Role role = roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName).orElseThrow();
        testRoleId = role.get_id().toHexString();
    }

    @Test
    @Order(2)
    void shouldFailToCreateDuplicateRole() throws Exception {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        mockMvc.perform(post("/api/auth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRole)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    void shouldCreateGrant() throws Exception {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription("Test grant for integration testing");

        mockMvc.perform(post("/api/auth/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantCreate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(testGrantName));

        Grant grant = grantRepository.findByName(testGrantName).orElseThrow();
        testGrantId = grant.get_id().toHexString();
    }

    @Test
    @Order(4)
    void shouldFailToCreateDuplicateGrant() throws Exception {
        GrantCreate grantCreate = new GrantCreate();
        grantCreate.setName(testGrantName);
        grantCreate.setDescription("Duplicate grant");

        mockMvc.perform(post("/api/auth/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantCreate)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(5)
    void shouldAddGrantToRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId);
        request.setGrantId(testGrantId);

        mockMvc.perform(post("/api/auth/role/grant")
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
    void shouldFailToAddDuplicateGrantToRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId);
        request.setGrantId(testGrantId);

        mockMvc.perform(post("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(7)
    void shouldRemoveGrantFromRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId);
        request.setGrantId(testGrantId);

        mockMvc.perform(delete("/api/auth/role/grant")
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
    void shouldReturn0WhenRemovingNonExistentGrantFromRole() throws Exception {
        RoleGrantRequest request = new RoleGrantRequest();
        request.setRoleId(testRoleId);
        request.setGrantId(testGrantId);

        mockMvc.perform(delete("/api/auth/role/grant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }
}
