package com.dashboard.oauth.service;

import com.dashboard.common.model.Audit;
import com.dashboard.common.model.exception.ConflictException;
import com.dashboard.common.model.exception.ResourceNotFoundException;
import com.dashboard.oauth.dataTransferObject.role.CreateRole;
import com.dashboard.oauth.dataTransferObject.role.RoleRead;
import com.dashboard.oauth.mapper.interfaces.IRoleMapper;
import com.dashboard.oauth.model.entities.Role;
import com.dashboard.oauth.repository.IRoleRepository;
import net.datafaker.Faker;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Role Service")
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private IRoleMapper roleMapper;

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
    @DisplayName("Get all roles")
    void getRoleById_shouldReturnRole_whenRoleExists() {
        when(roleRepository.findById(testRoleId)).thenReturn(Optional.of(testRole));

        Optional<Role> result = roleService.getRoleById(testRoleId);

        assertThat(result).isPresent();
        assertThat(result.get().get_id()).isEqualTo(testRoleId);
        verify(roleRepository).findById(testRoleId);
    }

    @Test
    @DisplayName("Get nonexistent role")
    void getRoleById_shouldReturnEmpty_whenRoleNotFound() {
        when(roleRepository.findById(testRoleId)).thenReturn(Optional.empty());

        Optional<Role> result = roleService.getRoleById(testRoleId);

        assertThat(result).isEmpty();
        verify(roleRepository).findById(testRoleId);
    }

    @Test
    @DisplayName("Get role by name")
    void getRoleByName_shouldReturnRole_whenRoleExists() {
        when(roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName))
                .thenReturn(Optional.of(testRole));

        Optional<Role> result = roleService.getRoleByName(testRoleName);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(testRoleName);
        verify(roleRepository).findByNameAndAudit_DeletedAtIsNull(testRoleName);
    }

    @Test
    @DisplayName("Get nonexistent role by name")
    void getRoleByName_shouldReturnEmpty_whenRoleNotFound() {
        when(roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName))
                .thenReturn(Optional.empty());

        Optional<Role> result = roleService.getRoleByName(testRoleName);

        assertThat(result).isEmpty();
        verify(roleRepository).findByNameAndAudit_DeletedAtIsNull(testRoleName);
    }

    @Test
    @DisplayName("Create role")
    void createRole_shouldReturnCreatedRole() {
        when(roleRepository.insert(any(Role.class))).thenReturn(testRole);

        Role result = roleService.createRole(testRole);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testRoleName);
        verify(roleRepository).insert(testRole);
    }

    @Test
    @DisplayName("Update role")
    void updateRole_shouldReturnUpdatedRole() {
        testRole.setName("UPDATED_ROLE");
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        Role result = roleService.updateRole(testRole);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("UPDATED_ROLE");
        verify(roleRepository).save(testRole);
    }

    @Test
    @DisplayName("Get all roles returns list")
    void getRoles_shouldReturnAllRoles() {
        RoleRead expectedRead = new RoleRead();
        expectedRead.setName(testRoleName);

        when(roleRepository.findAll()).thenReturn(List.of(testRole));
        when(roleMapper.toRead(testRole)).thenReturn(expectedRead);

        List<RoleRead> result = roleService.getRoles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(testRoleName);
    }

    @Test
    @DisplayName("Get role read by id returns DTO when role exists")
    void getRoleReadById_shouldReturnRoleRead_whenExists() {
        RoleRead expectedRead = new RoleRead();
        expectedRead.setName(testRoleName);

        when(roleRepository.findById(testRoleId)).thenReturn(Optional.of(testRole));
        when(roleMapper.toRead(testRole)).thenReturn(expectedRead);

        RoleRead result = roleService.getRoleReadById(testRoleId);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testRoleName);
    }

    @Test
    @DisplayName("Get role read by id throws ResourceNotFoundException when not found")
    void getRoleReadById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(roleRepository.findById(testRoleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleReadById(testRoleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Create role from DTO returns RoleRead when name not taken")
    void createRoleFromDto_shouldReturnRoleRead_whenNameNotTaken() {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        RoleRead expectedRead = new RoleRead();
        expectedRead.setName(testRoleName);

        when(roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName)).thenReturn(Optional.empty());
        when(roleRepository.insert(any(Role.class))).thenReturn(testRole);
        when(roleMapper.toRead(any(Role.class))).thenReturn(expectedRead);

        RoleRead result = roleService.createRole(createRole);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testRoleName);
        verify(roleRepository).insert(any(Role.class));
    }

    @Test
    @DisplayName("Create role from DTO throws ConflictException when name is taken")
    void createRoleFromDto_shouldThrowConflictException_whenNameTaken() {
        CreateRole createRole = new CreateRole();
        createRole.setName(testRoleName);

        when(roleRepository.findByNameAndAudit_DeletedAtIsNull(testRoleName))
                .thenReturn(Optional.of(testRole));

        assertThatThrownBy(() -> roleService.createRole(createRole))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Delete role removes entity when it exists")
    void deleteRole_shouldDeleteRole_whenExists() {
        when(roleRepository.findById(testRoleId)).thenReturn(Optional.of(testRole));

        roleService.deleteRole(testRoleId);

        verify(roleRepository).delete(testRole);
    }

    @Test
    @DisplayName("Delete role throws ResourceNotFoundException when not found")
    void deleteRole_shouldThrowResourceNotFoundException_whenNotFound() {
        when(roleRepository.findById(testRoleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.deleteRole(testRoleId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
