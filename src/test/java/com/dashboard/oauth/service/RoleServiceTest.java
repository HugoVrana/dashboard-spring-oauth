package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.repository.IRoleRepository;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private IRoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private final Faker faker = new Faker();

    private ObjectId testRoleId;
    private String testRoleName;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRoleId = new ObjectId();
        testRoleName = faker.expression("#{letterify 'ROLE_????'}").toUpperCase();
        testRole = createTestRole();
    }

    private Role createTestRole() {
        Role role = new Role();
        role.set_id(testRoleId);
        role.setName(testRoleName);
        role.setGrants(new ArrayList<>());

        Audit audit = new Audit();
        audit.setCreatedAt(Instant.now());
        role.setAudit(audit);

        return role;
    }

    @Test
    void getRoleById_shouldReturnRole_whenRoleExists() {
        when(roleRepository.findById(testRoleId)).thenReturn(Optional.of(testRole));

        Optional<Role> result = roleService.getRoleById(testRoleId);

        assertThat(result).isPresent();
        assertThat(result.get().get_id()).isEqualTo(testRoleId);
        verify(roleRepository).findById(testRoleId);
    }

    @Test
    void getRoleById_shouldReturnEmpty_whenRoleNotFound() {
        when(roleRepository.findById(testRoleId)).thenReturn(Optional.empty());

        Optional<Role> result = roleService.getRoleById(testRoleId);

        assertThat(result).isEmpty();
        verify(roleRepository).findById(testRoleId);
    }

    @Test
    void getRoleByName_shouldReturnRole_whenRoleExists() {
        when(roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName))
                .thenReturn(Optional.of(testRole));

        Optional<Role> result = roleService.getRoleByName(testRoleName);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(testRoleName);
        verify(roleRepository).findByNameAndAudit_DeletedAtIsNull(testRoleName);
    }

    @Test
    void getRoleByName_shouldReturnEmpty_whenRoleNotFound() {
        when(roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName))
                .thenReturn(Optional.empty());

        Optional<Role> result = roleService.getRoleByName(testRoleName);

        assertThat(result).isEmpty();
        verify(roleRepository).findByNameAndAudit_DeletedAtIsNull(testRoleName);
    }

    @Test
    void createRole_shouldReturnCreatedRole() {
        when(roleRepository.insert(any(Role.class))).thenReturn(testRole);

        Role result = roleService.createRole(testRole);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testRoleName);
        verify(roleRepository).insert(testRole);
    }

    @Test
    void updateRole_shouldReturnUpdatedRole() {
        testRole.setName("UPDATED_ROLE");
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        Role result = roleService.updateRole(testRole);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("UPDATED_ROLE");
        verify(roleRepository).save(testRole);
    }
}
